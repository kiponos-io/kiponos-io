---
title: "Tune CQRS Projection Lag Thresholds Live (Kiponos Python SDK)"
published: true
tags: python, microservices, architecture, realtime
description: Read-model staleness limits frozen in worker constants hide real incidents. Kiponos feeds live projection lag budgets and pause flags to Python projectors — zero-latency reads every poll loop.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-microservices-cqrs-projection-lag.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-microservices-cqrs.jpg
---

Customer support pulls order history from the **read model**. The write side processed the refund twenty minutes ago; the dashboard still shows "shipped" because the `order-summary` projector fell behind — and your worker still considers **30 seconds** "healthy" because `MAX_PROJECTION_LAG_SEC = 30` was set when daily event volume was a tenth of today's.

The support lead:

> "Agents are quoting **stale state** to customers. Can we **pause serving** the read API when lag exceeds five minutes — without a deploy?"

Projection lag tolerance is not a one-time SLO workshop outcome. It is **operational read-your-writes policy** that shifts when consumers break or when you intentionally throttle.

## Why CQRS projections break with static lag thresholds

Typical Python projector loop:

```python
MAX_PROJECTION_LAG_SEC = 30
PAUSE_SERVE_IF_LAG_EXCEEDS = 120

async def projection_loop() -> None:
    while True:
        lag = await measure_lag_seconds("order-summary")
        if lag > MAX_PROJECTION_LAG_SEC:
            metrics.gauge("projection.lag", lag)
        await process_batch()
        await asyncio.sleep(1)
```

Those constants usually come from:

1. **Module-level literals** — change means redeploy every projector worker
2. **Env vars in Docker** — ops cannot tune during consumer outage
3. **Separate alerting only** — dashboards turn red while the **read API keeps lying**

Lag checks run on **every projector tick** and on **read API guard paths**. You need local memory reads — same contract as [cross-service handoff timing](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-microservices-handoff.md).

## What teams believe

| What teams say | What production does |
|----------------|---------------------|
| "Projections are eventually consistent — 30s is fine" | Support tools need **tunable** staleness bounds |
| "Alerting is enough" | Alerts do not **stop** the read API from serving lies |
| "Lag is a consumer problem" | Read API should **degrade** when projector falls behind |
| "We'll scale projector pods" | Scaling without threshold tuning **masks** root cause |

## The Aha

**`MAX_PROJECTION_LAG_SEC = 30` feels like SLO documentation cast in code, but lag thresholds are operational guardrails** — tighten when Kafka is recovering, relax when you accept staleness during a planned replay. [Kiponos.io](https://kiponos.io) feeds `max_lag_sec`, `pause_serve_lag_sec`, and `batch_size` with local `get_int()` every loop — no redeploy, no worker restart.

## What is Kiponos.io (for CQRS projections)

[Kiponos.io](https://kiponos.io) holds projector policy under profile `['orders']['v2']['prod']['cqrs']` → `projections/order-summary`. WebSocket deltas update lag thresholds in projector workers **and** read API middleware simultaneously.

`kiponos.path("projections", "order-summary").get_int("max_lag_sec")` is a **local memory read** — no HTTP to an ops API while the consumer loop is already behind.

## Architecture: one tree, projectors and read API

![Architecture diagram](https://litter.catbox.moe/h91plg.png)

When ops raises `pause_serve_lag_sec`, **read API and projector** share the same live policy.

## CQRS projection config tree

```yaml
projections/
  order-summary/
    max_lag_sec: 30
    pause_serve_lag_sec: 300
    warn_lag_sec: 60
    batch_size: 200
    poll_interval_ms: 1000
    pause_projection: false
  customer-360/
    max_lag_sec: 120
    pause_serve_lag_sec: 600
    batch_size: 500
  global/
    emit_lag_metrics: true
    replay_mode: false
    replay_batch_size: 50
```

Each projector reads **its** subtree; global `replay_mode` slows consumption during event replay.

## Python integration (projector + read guard)

```python
import asyncio
import os
from kiponos import Kiponos

os.environ["KIPONOS_PROFILE"] = "['orders']['v2']['prod']['cqrs']"
kiponos = Kiponos.create_for_current_team()

async def projection_loop() -> None:
    while True:
        cfg = kiponos.path("projections", "order-summary")
        if cfg.get_bool("pause_projection"):
            await asyncio.sleep(1)
            continue
        lag = await measure_lag_seconds("order-summary")
        if lag > cfg.get_int("warn_lag_sec", 60):
            metrics.gauge("projection.lag", lag)
        global_cfg = kiponos.path("global")
        batch = (
            global_cfg.get_int("replay_batch_size", 50)
            if global_cfg.get_bool("replay_mode")
            else cfg.get_int("batch_size", 200)
        )
        await process_batch(batch)
        await asyncio.sleep(cfg.get_int("poll_interval_ms", 1000) / 1000)

def read_api_may_serve() -> bool:
    cfg = kiponos.path("projections", "order-summary")
    lag = current_lag_seconds("order-summary")
    return lag <= cfg.get_int("pause_serve_lag_sec", 300)
```

**Reads** (`get_int`, `get_bool`) are local — call them every loop iteration and on every read request guard.

Audit when ops changes lag policy mid-incident:

```python
kiponos.after_value_changed(
    lambda c: log.warning("Projection policy %s → %s", c.path, c.new_value)
)
```

## Real-world scenarios

| Scenario | Without Kiponos | With Kiponos |
|----------|-----------------|--------------|
| Kafka consumer lag spike | Support sees stale UI until deploy | Lower `pause_serve_lag_sec` — read API returns 503 with clear message |
| Planned event replay | Projectors overwhelm read DB | Enable `global.replay_mode`, shrink `replay_batch_size` live |
| Downstream DB maintenance | Projectors hammer sick replica | Set `pause_projection: true` in dashboard |
| Post-incident catch-up | Fixed batch size too small | Raise `batch_size` until lag drains |

## Performance

- **One WebSocket** per worker — not a config fetch per projection batch
- **Reads are O(1)** on the SDK cache — microseconds per loop tick
- **Delta patches** — changing `max_lag_sec` does not reload full CQRS tree
- **Shared tree** — read API guard and projector share policy without a side channel

## Compare to alternatives

| Approach | Mid-incident lag policy change | Per-loop read cost |
|----------|-------------------------------|---------------------|
| Module constants | Redeploy workers | Zero |
| Env var + pod restart | Rollout delay | Zero after restart |
| Redis config hash | Possible | RTT per loop |
| **Kiponos SDK** | **Dashboard** | **Zero (local)** |

## When not to use Kiponos

| Situation | Better approach |
|-----------|-----------------|
| Event schema versioning | Compatibility layer in code — structural |
| Source-of-truth writes | Command side validation — not projection tuning |
| Cross-region read replicas | Replication lag at database layer |
| Exactly-once projection semantics | Idempotent upsert design + dedup store |

## Getting started (15 minutes)

1. [Free TeamPro at kiponos.io](https://kiponos.io) — profile `projections/*` under `['orders']['v2']['prod']['cqrs']`
2. Connect projector and read API with `KIPONOS_ID`, `KIPONOS_ACCESS`, profile path
3. Replace `MAX_PROJECTION_LAG_SEC` with `kiponos.path("projections", "order-summary").get_int(...)`
4. Wire `read_api_may_serve()` before returning read model payloads
5. Staging: artificially delay consumer, lower `pause_serve_lag_sec`, confirm read API degrades without restart

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [Event bus routing live](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-microservices-event-routing.md)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

## What is next

Projection lag policy complements **outbox poll cadence** and **event routing pause flags** — write path, relay, and read path in one operational hub.

---

*Kiponos.io — real-time config for Python. Serve read models honestly while catch-up is in flight.*