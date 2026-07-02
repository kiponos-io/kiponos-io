---
title: "CELERY_WORKER_CONCURRENCY=4 Survived Every Scale-Up (Python + Kiponos)"
published: false
tags: python, celery, devops, performance
description: Task worker concurrency env vars feel set-once at deploy. When queue depth spikes, concurrency is operational — Kiponos feeds live worker policy.
canonical_url: https://dev.to/kiponos/celeryworkerconcurrency4-survived-every-scale-up-python-kiponos-m63
main_image: https://files.catbox.moe/ifp0w3.jpg
---

Invoice export backlog: **52,000** messages in RabbitMQ. Your Celery workers still run `--concurrency=4` because `CELERY_WORKER_CONCURRENCY=4` was written into `.env` when the fleet lived on a single `t3.small`.

Queue age crosses four hours. Finance escalates. Someone proposes "just add more pods" — but each pod still runs four child processes, CPU sits at 30%, and disk I/O on the PDF renderer is idle.

The platform engineer says:

> "Concurrency is **worker topology**. We set it at deploy and leave it."

Topology assumed 2022 traffic. Tonight's queue depth is not 2022. Concurrency is not topology — it is **how many tasks you dare run in parallel today**.

**The Aha:** read `concurrency` from [Kiponos.io](https://kiponos.io) in your worker bootstrap — ops raises to `16` live; new prefork children pick up the policy without a full redeploy.

## The problem: frozen concurrency on the task hot path

```python
# docker-compose.yml / k8s manifest — unchanged
# celery worker --concurrency=4

# tasks.py
from celery import Celery

app = Celery("exports")
CONCURRENCY = int(os.environ.get("CELERY_WORKER_CONCURRENCY", "4"))
```

Or hard-coded in `celeryconfig.py`. Pain:

1. **Queue spikes** — four slots while CPU is bored
2. **Env var change** — rolling restart of every worker deployment
3. **Burst then revert** — nobody remembers to scale concurrency back down

| What teams say | What production does |
|----------------|---------------------|
| "4 avoids OOM on PDF tasks" | OOM is per-task memory, not slot count tonight |
| "Autoscale handles it" | Autoscale still bounded by deploy-time max |
| "More replicas fix depth" | 10 pods × 4 = 40 slots; one pod × 16 = cheaper |

## The Aha: worker concurrency is today's queue drain dial

Store Celery policy under `celery/workers` in Kiponos. A bootstrap hook reads `concurrency` when spawning prefork children (or on `after_value_changed` triggers pool restart). Ops sets `concurrency: 16` during backlog burn-down; when the queue clears, ops returns to `4`.

**No full fleet redeploy** for a number that should move hourly.

## What Kiponos.io is — for Python task workers

[Kiponos.io](https://kiponos.io) connects your Celery worker process to a live config tree. `Kiponos.create_for_current_team()` with profile `['exports']['prod']['celery']` hydrates memory at startup. Dashboard edits are **WebSocket deltas**.

`kiponos.path("celery", "workers").get_int("concurrency")` before accepting tasks is a **local read** — no Redis poll per task dispatch.

`after_value_changed` can signal the worker to shrink or grow the prefork pool when ops edits concurrency bounds.

## Architecture

```mermaid
flowchart LR
    OPS["Platform ops<br/>Kiponos dashboard"] -->|WebSocket deltas| SDK["Python SDK in-mem<br/>Celery worker"]
    SDK -->|"get_int concurrency — local"| POOL["prefork pool<br/>task slots"]
    POOL --> QUEUE["RabbitMQ<br/>invoice export queue"]
```

## Example config tree

```yaml
celery/
  workers/
    concurrency: 4
    max_concurrency: 32
    min_concurrency: 2
    backlog_burn_mode: false
    burn_concurrency: 16
  tasks/
    pdf_export_time_limit_sec: 120
    soft_time_limit_sec: 90
  autoscale/
    enabled: false
    max: 24
    min: 4
```

## Python integration (Celery export worker)

```python
import logging
import os
from celery import Celery
from celery.signals import worker_process_init
from kiponos import Kiponos

log = logging.getLogger(__name__)

kiponos = Kiponos.create_for_current_team()
# Profile: ['exports']['prod']['celery'] via KIPONOS_PROFILE

app = Celery("exports")
app.config_from_object("celeryconfig")

def effective_concurrency() -> int:
    cfg = kiponos.path("celery", "workers")
    if cfg.get_bool("backlog_burn_mode", False):
        raw = cfg.get_int("burn_concurrency", 16)
    else:
        raw = cfg.get_int("concurrency", 4)
    lo = cfg.get_int("min_concurrency", 2)
    hi = cfg.get_int("max_concurrency", 32)
    return max(lo, min(hi, raw))

@worker_process_init.connect
def apply_live_concurrency(**kwargs):
    slots = effective_concurrency()
    log.info("Worker process starting with concurrency policy: %s", slots)

kiponos.after_value_changed(
    lambda change: log.warning(
        "Celery policy changed: %s → %s (new tasks use updated bounds)",
        change.path,
        change.new_value,
    )
    if change.path.startswith("celery/workers")
    else None
)

@app.task(bind=True, soft_time_limit=90)
def export_invoice_pdf(self, invoice_id: str) -> str:
    # Policy read local if task needs dynamic limits
    limit = kiponos.path("celery", "tasks").get_int("pdf_export_time_limit_sec", 120)
    return render_pdf(invoice_id, time_limit=limit)
```

Pair with a small supervisor script that restarts prefork children when `after_value_changed` fires on `concurrency` — or use Celery's `--autoscale` max bound from `celery/autoscale/max` read locally at worker boot.

Every `get_int()` is **local** — safe inside task bodies and dispatch loops.

## Real scenarios

| Moment | `CELERY_WORKER_CONCURRENCY=4` | Kiponos path |
|--------|------------------------------|--------------|
| 50k queue backlog | Four slots, CPU idle | `backlog_burn_mode: true`, `burn_concurrency: 16` |
| Queue drained Monday | Still running 16 — memory pressure | Disable burn mode live |
| PDF task OOM scare | Lower slots without chart edit | `concurrency: 2` from dashboard |
| Load test profile | Separate values file | Hub profile `['exports']['loadtest']['celery']` |

## Performance — why workers stay efficient

- One WebSocket per worker process — not a config fetch per task
- `get_int("concurrency")` at dispatch is O(1) on cached tree
- Delta updates — burn mode toggle patches two keys
- No container rebuild to change an integer
- `after_value_changed` runs async; task hot path only reads memory

## Compare to alternatives

| Approach | Raise concurrency during backlog | Per-task policy read |
|----------|----------------------------------|----------------------|
| `.env` concurrency | Rolling restart | Zero (frozen) |
| Kubernetes only scale replicas | Minutes + cost | Does not raise per-pod slots |
| Redis pub/sub config | Custom plumbing | Network RTT if polled per task |
| Celery autoscale alone | Bounded by deploy max | Still needs live max bound |
| **Kiponos SDK** | **Dashboard (seconds)** | **Memory read** |

## When not to use Kiponos for Celery concurrency

| Case | Better approach |
|------|-----------------|
| Broker URL / vhost credentials | Secrets manager |
| Task routing to dedicated queues | Architecture in code |
| Replacing Celery with Kafka consumers | Migration project |
| Concurrency 64 on memory-heavy tasks without profiling | Load test discipline |

## Getting started (15 minutes)

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['exports']['prod']['celery']`.
2. `pip install kiponos` — set `KIPONOS_ID`, `KIPONOS_ACCESS`, `KIPONOS_PROFILE`.
3. Create `celery/workers` with `concurrency`, `max_concurrency`, and `backlog_burn_mode`.
4. Wire `effective_concurrency()` into worker bootstrap and task time limits.
5. Game day: flood staging queue, enable `backlog_burn_mode` live, watch drain rate climb **without image rebuild**.

**Further reading:**

- [Developer Quickstart](https://dev.to/kiponos/kiponosio-developer-quickstart-java-python-and-your-first-live-config-change-3kjo)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — worker concurrency matches today's queue, not your first t3.small.*