---
title: "MAX_BATCH_WAIT_MS=50 Was GPU Gospel — We Widened It Live During the Latency Spike (Python)"
published: false
tags: python, ai, inference, gpu
description: Dynamic batching max-wait and batch size feel like compile-time constants. When GPU queues back up, those knobs are operational — Kiponos lets Python inference workers retune batching without restarts.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-ai-inference-batch-size.md
main_image: https://files.catbox.moe/n1iaiq.jpg
---

Embedding surge minute 14. Your `/v1/embed` fleet shows **P99 queue wait at 180ms** while GPU utilization sits at 41%. Someone tuned `MAX_BATCH_WAIT_MS = 50` during a cost sprint because smaller batches meant higher throughput per dollar — on paper.

The inference lead stares at Grafana:

> "Fifty milliseconds is our **batching contract**. We do not change max wait without a benchmark rerun."

But users are timing out on retrieval-augmented search. The GPUs are idle between micro-batches because requests arrive in a trickle, not a wave. **Max wait** is not architecture — it is how long you hold a single embedding request in the queue **tonight** while you trade latency for utilization.

Here is the click for most ML platform engineers:

**`max_batch_wait_ms` and `max_batch_size` behave like sacred constants in `batching.py`, but they are dials you need when traffic shape shifts.**

You can turn those dials **while inference workers keep serving** — no redeploy, no rolling restart across the GPU pool, no ConfigMap edit. The next batch formation cycle reads the new numbers from memory.

That is [Kiponos.io](https://kiponos.io).

## The problem: frozen batching policy on the GPU hot path

A typical Python embedding server does dynamic batching like this:

```python
# batching.py — unchanged since the "efficiency sprint"
MAX_BATCH_SIZE = 32
MAX_BATCH_WAIT_MS = 50
MIN_BATCH_SIZE = 1

async def collect_batch(queue: asyncio.Queue) -> list[Request]:
    batch = [await queue.get()]
    deadline = time.monotonic() + MAX_BATCH_WAIT_MS / 1000
    while len(batch) < MAX_BATCH_SIZE and time.monotonic() < deadline:
        try:
            batch.append(queue.get_nowait())
        except asyncio.QueueEmpty:
            await asyncio.sleep(0.001)
    return batch
```

Those constants usually come from:

1. **Module-level literals** — change means rebuild container image and drain GPU nodes
2. **Environment variables at pod start** — same rolling restart problem across a fleet
3. **Poll Redis for "batch policy"** — adds RTT inside the tightest loop in your server

The batch collector runs thousands of times per minute. You need **local reads** and **async updates** — exactly what Kiponos provides.

| What teams say | What production does |
|----------------|---------------------|
| "We benchmarked 50ms wait in the lab" | Traffic arrives in bursts, not uniform Poisson |
| "Larger batches always win on A100" | P99 user latency matters more than FLOPs/hour at 2 PM |
| "We'll tune it in the next model release" | Search users abandon during the queue |

The pain is not ignorance. Platform teams **know** batching is a latency–throughput tradeoff. They do not know there is a clean way to move the tradeoff **without recycling every inference worker**.

## The Aha: batching knobs are operational, not tattoos

Wire `max_batch_wait_ms`, `max_batch_size`, and related policy into Kiponos. Your worker still boots from minimal env config — but **live batching policy** lives in the hub:

```yaml
batching/
  embed/
    max_batch_size: 32
    max_batch_wait_ms: 50
    min_batch_size: 1
    pad_to_max: false
  latency_mode/
    enabled: false
    max_batch_wait_ms: 8
    max_batch_size: 8
```

During the incident, ops enables `latency_mode` and sets `max_batch_wait_ms` to `8`. WebSocket delivers a **delta** — only those keys patch into the SDK's in-memory tree. The next `collect_batch()` call reads `8` instead of `50`. **No restart.** GPUs stop idling between singleton batches because you moved an operational float, not redeployed a belief.

## What Kiponos.io is — for Python inference workers

[Kiponos.io](https://kiponos.io) is a real-time config hub with **Java** and **Python** SDKs. `Kiponos.create_for_current_team()` opens a WebSocket to `wss://kiponos.io/api/io-kiponos-sdk`, hydrates the tree for a profile like `['inference']['embed']['prod']['batching']`, and serves **local** `get_int()` / `get_bool()` on every batch formation.

Updates are **async deltas** — changing `max_batch_wait_ms` from `50` → `8` patches one node in memory. Your asyncio event loop never blocks on config I/O while the GPU waits.

No restart. No redeploy. No per-request object-store fetch. Profile path: `['app']['release']['env']['config']` or your team's inference namespace.

Optional `after_value_changed` logs policy flips or increments a Prometheus counter when ops toggles `latency_mode`.

## Architecture

![Architecture diagram](https://files.catbox.moe/pvm6vi.png)

1. **Connect once** at worker startup — one WebSocket per process lifetime.
2. **Full tree snapshot** loads for your inference profile.
3. **Dashboard edit** sends **delta only** — not a 40 KB YAML redeploy.
4. **SDK merges async** on a WebSocket worker thread.
5. **Reads are local** — batch formation never waits on the network.

## Example config tree

```yaml
batching/
  embed/
    max_batch_size: 32
    max_batch_wait_ms: 50
    min_batch_size: 1
    pad_to_max: false
    drop_oversized: true
  latency_mode/
    enabled: false
    max_batch_wait_ms: 8
    max_batch_size: 8
  throughput_mode/
    enabled: false
    max_batch_wait_ms: 120
    max_batch_size: 64
  limits/
    max_queue_depth: 2000
    shed_at_depth: 1800
```

## Python integration (dynamic batching server)

```python
import asyncio
import logging
import time
from kiponos import Kiponos

log = logging.getLogger(__name__)

kiponos = Kiponos.create_for_current_team()
# Profile: ['inference']['embed']['prod']['batching'] via KIPONOS_PROFILE

def _batch_cfg():
    if kiponos.path("batching", "latency_mode").get_bool("enabled", False):
        return kiponos.path("batching", "latency_mode")
    if kiponos.path("batching", "throughput_mode").get_bool("enabled", False):
        return kiponos.path("batching", "throughput_mode")
    return kiponos.path("batching", "embed")

def batch_policy() -> dict:
    cfg = _batch_cfg()
    return {
        "max_size": cfg.get_int("max_batch_size", 32),
        "max_wait_ms": cfg.get_int("max_batch_wait_ms", 50),
        "min_size": cfg.get_int("min_batch_size", 1),
    }

async def collect_batch(queue: asyncio.Queue) -> list:
    policy = batch_policy()  # local memory read — safe every iteration
    batch = [await queue.get()]
    deadline = time.monotonic() + policy["max_wait_ms"] / 1000
    while len(batch) < policy["max_size"] and time.monotonic() < deadline:
        try:
            batch.append(queue.get_nowait())
        except asyncio.QueueEmpty:
            await asyncio.sleep(0.001)
    return batch

async def inference_loop(queue: asyncio.Queue, model):
    while True:
        batch = await collect_batch(queue)
        tensors = stack_requests(batch)
        outputs = await model.forward(tensors)
        dispatch_results(batch, outputs)

kiponos.after_value_changed(
    lambda c: log.info("Batching policy changed: %s → %s", c.path, c.new_value)
    if c.path.startswith("batching/")
    else None
)
```

Every `get_int()` is a **local memory read** — microseconds, not milliseconds. Safe inside the tight asyncio loop that feeds your GPU.

Pair with [LLM inference serving](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-llm-inference-serving.md) when the same fleet also serves chat completions.

## Real scenarios

| Event | Frozen `MAX_BATCH_WAIT_MS=50` | Kiponos path |
|-------|-------------------------------|--------------|
| RAG launch traffic spike | P99 queue wait climbs; GPUs underutilized | Enable `latency_mode`, `max_batch_wait_ms: 8` live |
| Nightly batch re-embed job | Same policy wastes GPU on bursty offline work | Enable `throughput_mode`, `max_batch_size: 64` |
| Cost review week | Debate new Docker image per knob | Same container, hub profile `cost/efficiency` |
| Post-incident audit | Git blame on `batching.py` line 4 | Dashboard trail on `batching/embed` |

## Performance — why batching stays fast

- One WebSocket per inference worker — not one config fetch per batch
- `get_int("max_batch_wait_ms")` is O(1) on the cached tree — noise next to CUDA kernel time
- Delta updates — toggling `latency_mode` sends a handful of keys, not a full fleet rollout
- No GPU process restart — CUDA context and model weights stay loaded
- `after_value_changed` runs off the hot path; keep callbacks to logging/metrics only

## Compare to alternatives

| Approach | Change max wait during latency spike | Per-batch read cost |
|----------|--------------------------------------|---------------------|
| Module constant in `batching.py` | Image rebuild + rolling restart | Zero (frozen) |
| Kubernetes ConfigMap | Rolling pod restart on GPU nodes | Zero after restart |
| Poll Redis / etcd | Possible mid-flight | Network RTT × thousands of batches |
| Feature flag "latency mode" boolean | No numeric wait/size | Still need numbers somewhere |
| **Kiponos SDK** | **Dashboard (seconds)** | **Memory read** |

## When not to use Kiponos for batching policy

| Case | Better approach |
|------|-----------------|
| Switching model architecture (e.g. ONNX → TensorRT) | Git-reviewed deploy |
| GPU instance type change (A10 → L4) | Infrastructure / autoscaling |
| Replacing custom batcher with Triton dynamic batching | Server migration |
| `max_batch_size` above GPU memory — OOM | Capacity planning first |

## Getting started (15 minutes)

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['inference']['embed']['prod']['batching']`.
2. `pip install kiponos` — set `KIPONOS_ID`, `KIPONOS_ACCESS`, `KIPONOS_PROFILE`.
3. Create `batching/embed` with `max_batch_size`, `max_batch_wait_ms`, and sibling `latency_mode` folder.
4. Replace module-level `MAX_BATCH_WAIT_MS` with `batch_policy()` using `kiponos.path(...)`.
5. Game day: throttle synthetic RAG traffic in staging, enable `latency_mode` live, watch P99 queue wait drop **without worker restart**.

**Further reading:**

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — batch wait is tonight's latency tradeoff, not GPU gospel.*