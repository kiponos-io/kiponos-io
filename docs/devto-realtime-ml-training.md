---
title: "Tune Model Training in Real Time — Zero Latency, Zero Restarts (Kiponos Python SDK)"
published: true
tags: python, machinelearning, devops, realtime
description: Change learning rate, batch size, and regularization during a live training run. Kiponos pushes delta updates over WebSocket; your loop reads the latest values locally with no performance hit.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-realtime-ml-training.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-kiponos-announcement.jpg
---

Training jobs are supposed to be fire-and-forget. You pick hyperparameters, launch a GPU job, and wait hours for a learning curve that tells you what you should have changed on minute three.

What if you could change `learning_rate`, `weight_decay`, or `dropout` **while the epoch loop is running** — and the next batch already sees the new values?

That is what [Kiponos.io](https://kiponos.io) is built for: a real-time config hub where connected SDKs hold the latest values **in memory**, updated over a permanent WebSocket. No polling. No restart. No redeploy.

This article shows the pattern for **Python model training**: keep Kiponos inside your inner training loop, read parameters as plain local variables, and let operators (or another algorithm) push **delta-only** changes from the dashboard.

## The problem with "config files" in training loops

Most teams handle hyperparameters one of three ways:

1. **Static YAML at job start** — change means kill the run and lose progress.
2. **Environment variables** — same problem; baked in at process launch.
3. **Poll S3 / Redis / a REST endpoint** — works, but adds network latency and failure modes inside the hottest code path.

The inner loop runs millions of times. You cannot afford a remote call per step. You cannot afford to block on I/O while the GPU waits.

What you need is:

- Values that are **already local** when `for batch in loader` runs
- Updates that arrive **asynchronously** via WebSocket
- **Delta patches** only — not reloading a 50 MB config blob every time someone nudges learning rate

Kiponos does exactly that.

## How Kiponos stays out of your way

```
┌─────────────────┐     WebSocket (delta updates)     ┌──────────────────┐
│  Kiponos.io UI  │ ────────────────────────────────► │  Python SDK      │
│  (or API client)│                                   │  in-memory cache │
└─────────────────┘                                   └────────┬─────────┘
                                                               │ .get() — local read
                                                               ▼
                                                    ┌──────────────────┐
                                                    │  training loop   │
                                                    │  (no I/O wait)   │
                                                    └──────────────────┘
```

1. **Connect once** at trainer startup — `Kiponos.create_for_current_team()` opens `wss://kiponos.io/api/io-kiponos-sdk`.
2. **Initial tree load** — full config snapshot for your profile (e.g. `['ml-trainer']['v1']['prod']['hparams']`).
3. **Delta updates** — when an operator changes `learning_rate` from `3e-4` to `1e-4`, only that node is patched in memory.
4. **Reads are local** — `kiponos.path("optimizer").get_float("learning_rate")` returns the cached value. No network on the read path.

The training loop never blocks on config. The WebSocket worker applies changes in the background — the same architecture as the [Java SDK](https://github.com/kiponos-io/kiponos-io/tree/master/golden/java) golden example, now in your Python trainer.

## Example: PyTorch training loop with live hyperparameters

Organize training parameters under a Kiponos profile folder:

```
training/
  optimizer/
    learning_rate: 0.0003
    weight_decay: 0.01
  schedule/
    warmup_steps: 500
  regularization/
    dropout: 0.1
  control/
    max_epochs: 50
    early_stop_patience: 5
```

### Connect at startup

```python
import os
from kiponos import Kiponos  # Python SDK — same contract as Java

os.environ["KIPONOS_ID"] = "..."      # from kiponos.io Connect
os.environ["KIPONOS_ACCESS"] = "..."
os.environ["KIPONOS_PROFILE"] = "['ml-trainer']['v1']['prod']['hparams']"

kiponos = Kiponos.create_for_current_team()
```

### Read inside the loop — zero latency

```python
def current_hparams(kiponos):
    """Pure local reads — safe to call every batch."""
    opt = kiponos.path("training", "optimizer")
    reg = kiponos.path("training", "regularization")
    return {
        "lr": opt.get_float("learning_rate"),
        "weight_decay": opt.get_float("weight_decay"),
        "dropout": reg.get_float("dropout"),
    }

for epoch in range(max_epochs):
    hp = current_hparams(kiponos)  # local memory read
    for param_group in optimizer.param_groups:
        param_group["lr"] = hp["lr"]
        param_group["weight_decay"] = hp["weight_decay"]

    for batch in train_loader:
        # dropout modules can read hp["dropout"] each forward pass
        loss = train_step(model, batch, hp)
        ...
```

No `requests.get`. No file watch. No checkpoint reload. The next batch uses whatever value is in memory **right now**.

### Optional: react to changes explicitly

If you want to log or snapshot when a parameter flips:

```python
kiponos.after_value_changed(lambda change: print(
    f"[kiponos] {change.path} = {change.new_value}"
))
```

Callbacks fire on the WebSocket thread; keep them lightweight. The hot path stays `get()` from cache.

## What operators can change live

| Parameter | Why change mid-run |
|-----------|-------------------|
| Learning rate | Loss plateaued — decay early without restarting |
| Weight decay | Overfitting signal appeared at epoch 12 |
| Dropout | Regularization tweak during fine-tuning phase |
| Gradient clip max | Instability spike — tighten temporarily |
| Early-stop patience | Extend run when validation is still climbing |
| `max_epochs` | Stop sooner or allow longer run based on live metrics |

Each change is a **single delta** over WebSocket. The SDK merges it into the in-memory tree. Your loop does not know or care that someone edited the dashboard — it just reads fresher numbers.

## Performance: why there is no hit

- **One WebSocket** for the process lifetime — not one HTTP call per step.
- **Reads are dictionary lookups** on the SDK's cached tree — O(1) per key.
- **Updates are async** — the training thread never waits on the network.
- **Delta patches** — changing one float does not retransmit the whole config.

On a typical PyTorch GPU loop, the bottleneck remains `matmul`, not `kiponos.path(...).get_float(...)`. We have measured this in production-style trainers: config reads are noise next to backward pass cost.

## Compare to alternatives

| Approach | Mid-run changes | Read latency | Ops complexity |
|----------|-----------------|--------------|----------------|
| Static YAML | No | Zero | Low |
| Poll Redis/S3 | Yes | Network RTT per poll | Medium |
| Restart with new env | Yes (painful) | Zero after restart | High — lose state |
| **Kiponos SDK** | **Yes** | **Zero (local cache)** | **Low — dashboard + SDK** |

## Getting started

1. **Free TeamPro** at [kiponos.io](https://kiponos.io) — create a profile for your trainer (`ml-trainer` / env / `hparams`).
2. Add your hyperparameter tree in the dashboard.
3. Wire the Python SDK with `KIPONOS_ID`, `KIPONOS_ACCESS`, and profile path (same two-token contract as Java).
4. Replace hard-coded constants in your loop with `kiponos.path(...).get_*()` calls.
5. Run training, open the dashboard, change `learning_rate`, watch the next steps use it.

Open-source integration resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io) — golden Java example, Agent Skills, and `AGENTS.md` for coding assistants.

## What is next

The natural follow-up: a **supervisor process** — another algorithm watching validation loss, gradient norms, or GPU telemetry — that writes parameter adjustments back through Kiponos (or the API) while the trainer keeps reading locally. Same zero-latency read path; closed-loop control without touching the training binary.

That is orchestration at runtime, not a new deployment.

---

*Kiponos.io — real-time config for Java and Python. Change values in the browser; running code sees them instantly.*