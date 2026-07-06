---
title: "LEARNING_RATE=2e-5 Was the Experiment Contract — We Warmed Up Live on Epoch 3 (Python)"
published: false
tags: python, machinelearning, finetuning, ai
description: Fine-tune learning rate and warmup steps feel frozen at job submit. Experimental runs need mid-epoch course correction — Kiponos lets Python trainers read live hyperparameters without killing GPU progress.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-ai-finetune-hyperparams-live.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-ai-finetune-hyperparams-live.jpg
---

Fine-tune run `ft-support-bot-v7` hour 6. Loss flattened after epoch 2 — validation perplexity stuck at 4.1 while your target was 3.6 before the executive demo Friday. The run used `LEARNING_RATE = 2e-5` and `WARMUP_STEPS = 100`, copied from a successful legal-domain fine-tune that had **ten times more training data**.

The ML lead messages the channel:

> "Learning rate is part of the **experiment contract**. We do not change it mid-run — that invalidates the comparison."

But invalidating the comparison is cheaper than invalidating the demo. The GPU has been running for six hours. Killing the job loses checkpoint state, queue position, and the only window before the board review. **Learning rate and warmup** are not sacred experiment metadata — they are **operational knobs** on a live training loop that should respond to what the loss curve is doing *right now*.

Here is the click for most applied ML teams:

**`learning_rate` and `warmup_steps` behave like constants in `train.py`, but they are dials you need during experimental fine-tune runs.**

You can nudge those dials **while the epoch loop keeps running** — no job kill, no resubmit to the cluster scheduler, no editing a mounted ConfigMap and praying the sidecar reloads. The **next batch** reads the new learning rate from memory.

That is [Kiponos.io](https://kiponos.io).

## The problem: hyperparameters frozen at `torchrun` launch

A typical LoRA fine-tune script hard-codes or argparse-bakes hyperparameters once:

```python
# train.py — frozen at job submit
LEARNING_RATE = 2e-5
WARMUP_STEPS = 100
WEIGHT_DECAY = 0.01
MAX_EPOCHS = 5

optimizer = AdamW(model.parameters(), lr=LEARNING_RATE, weight_decay=WEIGHT_DECAY)
scheduler = get_linear_schedule_with_warmup(optimizer, WARMUP_STEPS, total_steps)
```

Teams handle mid-run changes one of three ways:

1. **Kill and resubmit** — lose hours of GPU time and queue depth
2. **Manual SSH + edit running script** — not reproducible, not auditable
3. **Poll S3/Redis inside the inner loop** — network latency while the GPU waits between steps

The inner loop runs millions of iterations. You need **local reads** and **async WebSocket updates**.

| What teams say | What production does |
|----------------|---------------------|
| "Hyperparams are fixed for reproducibility" | Reproducibility without convergence is worthless |
| "We'll sweep LR in the next experiment grid" | The demo is Thursday |
| "Warmup is derived from dataset size" | This dataset is 8k rows, not 800k |

Platform engineers **know** learning rate schedules matter. They do not know there is a clean way to adjust them **without terminating the training process**.

## The Aha: fine-tune hyperparameters are operational during experiments

Wire `learning_rate`, `warmup_steps`, `weight_decay`, and schedule overrides into Kiponos. The trainer still boots from minimal cluster env — but **live hyperparameter policy** lives in the hub:

```yaml
finetune/
  optimizer/
    learning_rate: 0.00002
    weight_decay: 0.01
    adam_beta2: 0.999
  schedule/
    warmup_steps: 100
    warmup_restart_steps: 0
    min_lr_ratio: 0.1
  experiment/
    run_id: ft-support-bot-v7
    allow_mid_run_lr_change: true
  safety/
    max_learning_rate: 0.0001
    min_learning_rate: 0.000001
```

At epoch 3, the researcher lowers `learning_rate` to `8e-6` and raises `warmup_steps` to `200` for a gentle restart. WebSocket delivers **deltas**. The next `current_hparams()` call returns the new values; optimizer param groups update before the next forward pass. **No job kill.**

## What Kiponos.io is — for Python fine-tune trainers

[Kiponos.io](https://kiponos.io) is a real-time config hub with **Java** and **Python** SDKs. `Kiponos.create_for_current_team()` opens WebSocket, loads the tree for a profile like `['finetune']['support-bot']['exp']['hparams']`, and serves **local** `get_float()` / `get_int()` inside your training loop.

Updates are **async deltas** — nudging `learning_rate` patches one key in memory. The GPU never blocks on config I/O between batches.

No restart. No redeploy. No per-step remote fetch. Same mechanism as [realtime ML training](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-realtime-ml-training.md), tuned for experimental fine-tune workflows.

Optional `after_value_changed` logs who changed LR and when — critical for experiment audit trails.

## Architecture

![Architecture diagram](https://files.catbox.moe/5rftdr.png)

1. **Connect once** at `torchrun` startup.
2. **Full tree snapshot** for fine-tune profile.
3. **Dashboard nudge** sends **delta only**.
4. **SDK merges async** — training thread unaffected.
5. **Reads are local** — safe every batch.

## Example config tree

```yaml
finetune/
  optimizer/
    learning_rate: 0.00002
    weight_decay: 0.01
    adam_beta2: 0.999
    gradient_clip_norm: 1.0
  schedule/
    warmup_steps: 100
    warmup_restart_steps: 0
    min_lr_ratio: 0.1
    cosine_restarts: false
  lora/
    rank: 16
    alpha: 32
    dropout: 0.05
  experiment/
    run_id: ft-support-bot-v7
    allow_mid_run_lr_change: true
    target_val_perplexity: 3.6
  safety/
    max_learning_rate: 0.0001
    min_learning_rate: 0.000001
```

## Python integration (LoRA fine-tune trainer)

```python
import logging
from kiponos import Kiponos

log = logging.getLogger(__name__)

kiponos = Kiponos.create_for_current_team()
# Profile: ['finetune']['support-bot']['exp']['hparams'] via KIPONOS_PROFILE

def current_hparams() -> dict:
    opt = kiponos.path("finetune", "optimizer")
    sched = kiponos.path("finetune", "schedule")
    safety = kiponos.path("finetune", "safety")
    lr = opt.get_float("learning_rate", 2e-5)
    lr = max(safety.get_float("min_learning_rate", 1e-6),
             min(lr, safety.get_float("max_learning_rate", 1e-4)))
    return {
        "lr": lr,
        "weight_decay": opt.get_float("weight_decay", 0.01),
        "warmup_steps": sched.get_int("warmup_steps", 100),
        "grad_clip": opt.get_float("gradient_clip_norm", 1.0),
    }

def apply_hparams(optimizer, hp: dict) -> None:
    for group in optimizer.param_groups:
        group["lr"] = hp["lr"]
        group["weight_decay"] = hp["weight_decay"]

def train_epoch(model, optimizer, train_loader, global_step: int):
    for batch in train_loader:
        hp = current_hparams()  # local memory read — every batch
        apply_hparams(optimizer, hp)
        loss = train_step(model, batch, hp)
        loss.backward()
        torch.nn.utils.clip_grad_norm_(model.parameters(), hp["grad_clip"])
        optimizer.step()
        optimizer.zero_grad()
        global_step += 1
        if should_warmup(global_step, hp["warmup_steps"]):
            adjust_warmup_lr(optimizer, global_step, hp)
    return global_step

kiponos.after_value_changed(
    lambda c: log.info("Fine-tune hparam changed: %s → %s", c.path, c.new_value)
    if c.path.startswith("finetune/")
    else None
)
```

Every `get_float()` is a **local memory read** — safe inside the hottest loop in your trainer.

Pair with [supervisor ML training](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-supervisor-ml-training.md) when a coordinator service watches loss and suggests hub updates.

## Real scenarios

| Event | Frozen `LEARNING_RATE=2e-5` | Kiponos path |
|-------|-----------------------------|--------------|
| Loss plateau epoch 3 | Kill job, lose 6 GPU-hours | Lower `learning_rate` live, extend `warmup_steps` |
| Overfitting spike on val set | Wait for next experiment | Raise `weight_decay`, clip harder via hub |
| Demo deadline moved up | Rush new argparse grid | Same run, hub profile `demo/aggressive` |
| Reproducibility audit | Notebook says 2e-5; run used 8e-6 after manual SSH | Dashboard change log with timestamps |

## Performance — why the training loop stays fast

- One WebSocket per trainer process — not one S3 fetch per step
- `get_float("learning_rate")` is O(1) on cached tree — invisible vs backward pass
- Delta updates — LR nudge sends one float, not reloading full experiment YAML
- No process restart — CUDA context, optimizer state, and DataLoader workers stay alive
- `after_value_changed` on background thread; never block GPU step

## Compare to alternatives

| Approach | Change LR on epoch 3 | Per-batch read cost |
|----------|----------------------|---------------------|
| Argparse at job submit | Kill and resubmit | Zero (frozen) |
| MLflow param store poll | Possible | Network RTT × millions of steps |
| Jupyter `%run` magic mid-flight | Manual, not fleet-safe | N/A |
| Ray Tune scheduler | Good for sweeps, heavy for one run | Scheduler overhead |
| **Kiponos SDK** | **Dashboard (seconds)** | **Memory read** |

## When not to use Kiponos for fine-tune hyperparameters

| Case | Better approach |
|------|-----------------|
| Architecture change (LoRA rank, new base model) | Git-reviewed training code |
| Dataset swap (new JSONL corpus) | New job with versioned data path |
| Full distributed hyperparameter sweep (100+ trials) | Ray Tune / Optuna |
| Publishing final model card with frozen hparams | Export hub snapshot to Git after convergence |

## Getting started (15 minutes)

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['finetune']['support-bot']['exp']['hparams']`.
2. `pip install kiponos` — set `KIPONOS_ID`, `KIPONOS_ACCESS`, `KIPONOS_PROFILE`.
3. Create `finetune/optimizer` and `finetune/schedule` with `learning_rate`, `warmup_steps`, and `safety` bounds.
4. Replace argparse-only LR with `current_hparams()` called each batch (or each optimizer step).
5. Game day: start a short fine-tune in staging, watch loss plateau, lower `learning_rate` from dashboard, confirm very next step uses new LR **without job restart**.

**Further reading:**

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — fine-tune learning rate is a live dial, not a job-submit contract.*