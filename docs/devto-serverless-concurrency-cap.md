---
title: "Lambda Concurrency Caps You Can Tighten at 2 AM — Without Terraform Apply (Kiponos Python SDK)"
published: false
tags: python, serverless, aws, finops
description: reserved_concurrency and in-process parallelism are frozen in Terraform and handler code. When a webhook flood threatens downstream Postgres, concurrency is operational — Kiponos feeds live caps to Python Lambda handlers.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-serverless-concurrency-cap.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-serverless-concurrency-cap.jpg
---

Thursday 02:06 UTC. A partner misconfigured their webhook retry policy. Your `invoice-events` Lambda fleet jumps from **200 invocations/minute** to **14,000**. Downstream Postgres connection count hits **max_connections** before CloudWatch alarms wake anyone. Terraform still says `reserved_concurrency = 500` — written when this function was a trickle, not a firehose.

The serverless lead types in the war room:

> "Drop concurrency to **50** now. We'll Terraform it properly tomorrow."

`aws lambda put-function-concurrency` works — but your **in-handler** fan-out still allows fifty parallel `asyncio` tasks each opening a DB pool connection. Reserved concurrency is only the **AWS envelope**. The **effective parallelism** inside the handler is `MAX_INFLIGHT = 32` in `handler.py`, shipped in the last container image **six weeks ago**.

Two knobs. Two deploy paths. One database melting.

**The Aha:** store **both** the AWS-facing cap and the in-process semaphore limit in [Kiponos.io](https://kiponos.io). A tiny Python **concurrency-adjuster** (Lambda layer or sidecar cron) reads `serverless/concurrency/*` and calls `put_function_concurrency` when ops changes `reserved_concurrency`. The handler reads `max_inflight_per_invocation` locally on every batch — **no image rebuild, no Terraform plan**.

## The problem — frozen concurrency at two layers

```python
# handler.py — image artifact
import asyncio
import os

MAX_INFLIGHT = int(os.environ.get("MAX_INFLIGHT", "32"))
DB_POOL_SIZE = 8

_semaphore = asyncio.Semaphore(MAX_INFLIGHT)

async def process_record(record: dict) -> None:
    async with _semaphore:
        await write_invoice_event(record)
```

```hcl
# terraform/invoice_events.tf
resource "aws_lambda_function" "invoice_events" {
  reserved_concurrent_executions = 500
}
```

| What teams believe | What production does |
|--------------------|---------------------|
| "Reserved concurrency protects RDS" | 500 × 8 pool connections still exceeds RDS |
| "SQS batching limits parallelism" | Handler spawns tasks inside one invocation |
| "We'll lower TF in the morning" | Morning is **$4k connection surge** bill |
| "Lambda scales to zero — cost is fine" | **Downstream** cost is not fine |

## What Kiponos.io is — for serverless concurrency

[Kiponos.io](https://kiponos.io) connects Python runtimes to a live config tree via WebSocket. `Kiponos.create_for_current_team()` hydrates memory at cold start and on warm invocations. Dashboard edits are **delta patches**.

Profile:

```
['webhooks']['prod']['serverless']
```

`kiponos.path("serverless", "concurrency").get_int("max_inflight_per_invocation")` inside `process_record` is a **local read** — no Parameter Store poll per record, no SSM latency on the hot path.

Pair with [Python semaphore tuning](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-python-semaphore.md) for worker fleets — this article owns **Lambda + AWS concurrency API**.

## Architecture

![Architecture diagram](https://files.catbox.moe/k70exj.png)

## Config tree

```yaml
serverless/
  concurrency/
    reserved_concurrency: 500
    max_inflight_per_invocation: 32
    min_reserved_concurrency: 10
    max_reserved_concurrency: 500
    emergency_throttle_mode: false
    emergency_reserved_concurrency: 50
    emergency_max_inflight: 4
  downstream/
    db_pool_size_per_invocation: 8
    max_db_connections_budget: 120
    backoff_ms_on_saturation: 250
  flood/
    partner_retry_storm_detected: false
    shed_percentage: 0
    dlq_overflow_threshold: 10000
  audit/
    last_change_reason: ""
```

## Integration — handler + adjuster

```python
import asyncio
import logging
import os
from typing import Any

import boto3
from kiponos import Kiponos

log = logging.getLogger(__name__)

os.environ.setdefault("KIPONOS_PROFILE", "['webhooks']['prod']['serverless']")
kiponos = Kiponos.create_for_current_team()

_semaphore: asyncio.Semaphore | None = None
_semaphore_limit: int = 0


def effective_limits() -> tuple[int, int]:
    cfg = kiponos.path("serverless", "concurrency")
    if cfg.get_bool("emergency_throttle_mode", False):
        reserved = cfg.get_int("emergency_reserved_concurrency", 50)
        inflight = cfg.get_int("emergency_max_inflight", 4)
    else:
        reserved = cfg.get_int("reserved_concurrency", 500)
        inflight = cfg.get_int("max_inflight_per_invocation", 32)
    lo = cfg.get_int("min_reserved_concurrency", 10)
    hi = cfg.get_int("max_reserved_concurrency", 500)
    return max(lo, min(hi, reserved)), max(1, inflight)


def _ensure_semaphore() -> asyncio.Semaphore:
    global _semaphore, _semaphore_limit
    _, inflight = effective_limits()
    if _semaphore is None or _semaphore_limit != inflight:
        _semaphore = asyncio.Semaphore(inflight)
        _semaphore_limit = inflight
        log.warning("Semaphore resized to max_inflight=%s", inflight)
    return _semaphore


kiponos.after_value_changed(
    lambda c: _ensure_semaphore()
    if c.path.startswith("serverless/concurrency")
    else None
)


async def process_record(record: dict[str, Any]) -> None:
    sem = _ensure_semaphore()
    async with sem:
        pool_size = kiponos.path("serverless", "downstream").get_int(
            "db_pool_size_per_invocation", 8
        )
        await write_invoice_event(record, pool_size=pool_size)


async def async_handler(event: dict, context: Any) -> dict:
    records = event.get("Records", [])
    await asyncio.gather(*(process_record(r) for r in records))
    return {"processed": len(records)}


def handler(event: dict, context: Any) -> dict:
    return asyncio.get_event_loop().run_until_complete(async_handler(event, context))
```

Adjuster (runs on EventBridge every minute or on hub webhook):

```python
import boto3
from kiponos import Kiponos

kiponos = Kiponos.create_for_current_team()
lambda_client = boto3.client("lambda")
FUNCTION_NAME = "invoice-events"


def sync_aws_reserved_concurrency() -> None:
    reserved, _ = effective_limits()
    lambda_client.put_function_concurrency(
        FunctionName=FUNCTION_NAME,
        ReservedConcurrentExecutions=reserved,
    )


kiponos.after_value_changed(
    lambda c: sync_aws_reserved_concurrency()
    if c.path.startswith("serverless/concurrency")
    else None
)
```

Ops flips `emergency_throttle_mode: true` → reserved **50**, inflight **4** → AWS and in-process limits align within one adjuster tick — **same Lambda image**.

## Real scenarios

| Event | Frozen TF + `MAX_INFLIGHT=32` | Kiponos serverless tree |
|-------|------------------------------|-------------------------|
| Partner retry storm | RDS max_connections | `emergency_throttle_mode: true` |
| Storm ends | Manual TF revert Monday | Disable emergency mode live |
| Black Friday prep | Separate TF workspace | Raise `reserved_concurrency` in hub |
| Load test profile | New image tag | Profile `['webhooks']['loadtest']['serverless']` |
| DLQ depth spike | Blind scaling | `shed_percentage` + lower inflight |

## Performance

- `get_int("max_inflight_per_invocation")` per record: **O(1) memory** — no SSM on hot path
- One WebSocket per **warm** execution environment — amortized across invocations
- Semaphore resize only on **delta** — not every SQS message
- Adjuster: one `put_function_concurrency` per policy change — not per invocation
- Cold start: full tree snapshot once; warm invocations skip network for reads

## Compare to alternatives

| Approach | 2 AM emergency throttle | In-handler inflight control |
|----------|-------------------------|----------------------------|
| Terraform only | `apply` + state lock | Does not change semaphore |
| AWS Console concurrency | Fast, no audit | Handler still runs 32 parallel |
| SSM Parameter Store poll | No TF | Network per read if polled |
| API Gateway throttle only | Edge limit | Lambda still fans out inside |
| **Kiponos SDK** | **Dashboard + adjuster** | **Local semaphore read** |

## When not to use Kiponos for Lambda concurrency

| Case | Better approach |
|------|-----------------|
| IAM role ARNs, KMS keys | AWS Secrets Manager / SSM SecureString |
| Function memory MB and timeout architecture | Terraform — changes are rare |
| VPC subnet and security group wiring | GitOps IaC |
| Replacing Lambda with K8s workers | Migration — see [Python worker concurrency](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-python-worker-concurrency.md) |

## Getting started (15 minutes)

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['webhooks']['prod']['serverless']`.
2. `pip install kiponos` in Lambda layer; set `KIPONOS_ID`, `KIPONOS_ACCESS`.
3. Create `serverless/concurrency` with `reserved_concurrency` and `max_inflight_per_invocation`.
4. Wire `_ensure_semaphore()` and deploy **concurrency-adjuster** with `lambda:PutFunctionConcurrency`.
5. Game day: flood staging queue, enable `emergency_throttle_mode`, confirm RDS connections flatten **without** `terraform apply`.
6. Post-incident: sync final values to Terraform — hub for **break-glass**, Git for **baseline**.

**Further reading:**

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [Python semaphore limits](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-python-semaphore.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — serverless concurrency matches tonight's downstream budget, not launch-day Terraform.*