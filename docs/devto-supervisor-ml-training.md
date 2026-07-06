---
title: "A Supervisor Algorithm That Retunes Your Model Training in Real Time (Kiponos Python SDK)"
published: true
tags: python, machinelearning, ai, realtime
description: Run a second process that watches validation loss and gradient health, then pushes hyperparameter deltas through Kiponos while the trainer reads them locally with zero latency.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-supervisor-ml-training.md
main_image: https://files.catbox.moe/s42jq2.jpg
---

In [the previous article](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-realtime-ml-training.md) we showed how a **training loop** can read live hyperparameters from [Kiponos.io](https://kiponos.io) with zero latency — local memory reads, WebSocket delta updates, no restart.

This article goes one level deeper: a **supervisor algorithm** — a separate process — that watches the run and **writes** parameter adjustments back through Kiponos while training keeps going.

Two processes. One config hub. The trainer never blocks on I/O.

## The architecture

![Architecture diagram](https://files.catbox.moe/ehz7wa.png)

| Role | Responsibility | Performance constraint |
|------|----------------|------------------------|
| **Trainer** | Forward/backward, batch loop | Must not block on network |
| **Supervisor** | Observe metrics, decide adjustments | Can take seconds between decisions |
| **Kiponos** | Single source of truth for hparams | Delta-only patches to connected SDKs |

The trainer **only reads**. The supervisor **only writes** (via dashboard, API, or its own Kiponos SDK client). Neither process talks to the other directly — they coordinate through the config tree.

## Why not have the trainer self-tune?

You can embed a learning-rate scheduler in the training script. That works for deterministic schedules (`StepLR`, cosine decay). It breaks down when:

- Tuning logic is **complex** — multi-signal rules (loss plateau + gradient spike + GPU memory pressure)
- You want **human override** mid-run without touching the trainer binary
- The policy team ships **new rules** without redeploying the GPU job
- A **different team** owns operational control vs. model architecture

Separating supervisor from trainer keeps the hot path minimal: `kiponos.path("training", "optimizer").get_float("learning_rate")` and nothing else.

## Shared config tree

Structure the Kiponos profile so both processes use the same namespace:

```yaml
training/
  optimizer/
    learning_rate: 0.0003
    weight_decay: 0.01
  regularization/
    dropout: 0.1
  control/
    max_epochs: 50
    early_stop: false
  telemetry/
    last_val_loss: 0.0      # trainer writes (optional)
    last_grad_norm: 0.0
    epoch: 0
```

- **Trainer reads** `training/optimizer/*`, `training/regularization/*`, `training/control/*`
- **Supervisor reads** `training/telemetry/*` (or external metrics store)
- **Supervisor writes** `training/optimizer/*`, `training/control/*`

Telemetry keys can be updated by the trainer on a slow cadence (end of epoch) — not every batch.

## Trainer side (unchanged hot path)

```python
from kiponos import Kiponos

kiponos = Kiponos.create_for_current_team()

def hparams():
    opt = kiponos.path("training", "optimizer")
    return {
        "lr": opt.get_float("learning_rate"),
        "wd": opt.get_float("weight_decay"),
        "dropout": kiponos.path("training", "regularization").get_float("dropout"),
    }

for epoch in range(1000):
    hp = hparams()  # local read — zero latency
    for batch in loader:
        loss = train_step(model, batch, hp)
    val_loss = validate(model)
    # Optional: push telemetry once per epoch (not in inner loop)
    kiponos.path("training", "telemetry").set("last_val_loss", val_loss)
    kiponos.path("training", "telemetry").set("epoch", epoch)
```

The inner loop still never waits on the network. Telemetry writes happen **once per epoch** — acceptable overhead.

## Supervisor side (policy loop)

The supervisor runs on CPU, polls metrics every N seconds, applies rules, and **updates Kiponos values** (via SDK write or dashboard). Pseudocode:

```python
import time
from kiponos import Kiponos

kiponos = Kiponos.create_for_current_team()
opt = kiponos.path("training", "optimizer")
tel = kiponos.path("training", "telemetry")
ctl = kiponos.path("training", "control")

PREV_LOSS = None

while not ctl.get_bool("early_stop"):
    time.sleep(30)  # supervisor cadence — not per-batch

    val_loss = tel.get_float("last_val_loss")
    grad_norm = tel.get_float("last_grad_norm")
    lr = opt.get_float("learning_rate")

    # Rule 1: plateau — decay learning rate
    if PREV_LOSS and abs(val_loss - PREV_LOSS) < 1e-4:
        opt.set("learning_rate", lr * 0.5)
        print(f"[supervisor] plateau → lr {lr} → {lr * 0.5}")

    # Rule 2: gradient explosion — tighten clip, reduce lr
    if grad_norm > 10.0:
        opt.set("learning_rate", lr * 0.25)
        kiponos.path("training", "optimizer").set("grad_clip_max", 0.5)

    # Rule 3: divergence — stop
    if val_loss > 5.0:
        ctl.set("early_stop", True)
        print("[supervisor] divergence → early_stop=true")

    PREV_LOSS = val_loss
```

Each `opt.set(...)` becomes a **delta patch** over WebSocket to every connected SDK — including the trainer. On the trainer's next `hparams()` call, it sees the new `learning_rate`.

No IPC. No shared filesystem. No trainer restart.

## Human in the loop

The same config tree is editable in the **Kiponos.io dashboard**. An operator can:

- Override the supervisor's last decision
- Pause automatic writes by freezing a folder
- Bump `max_epochs` when validation is still improving

The supervisor and the human use the same control plane. The trainer does not know who changed the value.

## Performance: still zero hit on the hot path

| Operation | Where | Cost |
|-----------|-------|------|
| `get_float("learning_rate")` per batch | Trainer | Local cache lookup |
| WebSocket delta | Background thread | Async, no trainer wait |
| `set("learning_rate", ...)` | Supervisor | Network write, ~ms; trainer unaffected |
| Telemetry `set` once per epoch | Trainer | One write per epoch, not per step |

The supervisor can be wrong, slow, or restarted — the trainer keeps running with the last known good values in memory until a delta arrives.

## When to use Java vs Python SDK

This pattern is **language-agnostic**:

- **Python trainer + Python supervisor** — most common for ML shops
- **Python trainer + Java supervisor** — enterprise orchestration service writing via Java SDK
- **Java inference service + Python supervisor** — mixed stacks share one Kiponos profile

Same WebSocket, same delta semantics, same zero-latency reads.

## Getting started

1. [Free TeamPro at kiponos.io](https://kiponos.io) — one profile for `training/*`
2. Wire the trainer with read-only `get_*` in the loop (see [previous article](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-realtime-ml-training.md))
3. Run a supervisor script on CPU with your policy rules and `set()` calls
4. Open the dashboard — watch values change live as both processes run

Open-source integration resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

## What is next

Closed-loop training control is one pattern. The same hub drives **live fraud thresholds** in Java payment services, **rate limits** on API gateways, and **A/B weights** in checkout — any runtime that must read fresh config without redeploying.

---

*Kiponos.io — real-time config for Java and Python. Supervise, orchestrate, and retune running systems from one live config tree.*