---
title: "Warehouse Sync Batch Sizes Live — Tune ETL Throughput Overnight (Python SDK)"
published: false
tags: python, data, etl, architecture
description: Batch size in Airflow variables is ops-blind during failures. Kiponos holds batch rows per pipeline stage.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-dataops-warehouse-sync-batch-sizes.md
main_image: https://files.catbox.moe/e166i8.jpg
---

Sunday 23:47 UTC. The **nightly warehouse sync** for `fact_orders` stalls — Snowflake warehouse **COMPUTE_WH** shows query queue depth at 14, lock waits on the hot table. The Celery worker still reads `BATCH_ROWS = 5000` from `sync_config.py`, sized when the table was half its current row width.

The on-call data engineer needs `batch_row_count` at **1200** for the remaining six-hour window — not an Airflow Variable PR that misses tonight's DAG run. Platform ops asks:

> "Batch size is an **overnight ops knob** — why is it trapped in **module constants** while the warehouse is choking?"

Most Python warehouse sync workers encode batch policy as **module constants**, **Airflow Variables**, and **hard-coded `LIMIT 5000`** in SQL builders. [Kiponos.io](https://kiponos.io) holds per-table batch sizes in profile `['warehouse']['prod']['sync']` with **local `get_int()` every batch loop**.

## The problem: batch_row_count frozen in sync workers

```python
# sync_config.py
BATCH_ROWS = 5000

def sync_table(cursor, table: str, watermark: datetime) -> int:
    while True:
        rows = cursor.execute(
            f"SELECT * FROM {table} WHERE updated_at > %s LIMIT %s",
            (watermark, BATCH_ROWS),
        ).fetchall()
        if not rows:
            break
        warehouse_loader.upsert_batch(table, rows)
    return len(rows)
```

Airflow Variable — parse-time only:

```python
batch_rows = Variable.get("WAREHOUSE_BATCH_ROWS", default_var=5000)
```

During warehouse slowdown you need to:

1. Lower **`tables.fact_orders.batch_row_count`** to 1200
2. Keep **`tables.dim_customers.batch_row_count`** at 5000
3. Enable **`throttle.on_queue_depth`** to auto-shrink batches

Editing the DAG repo at midnight misses the running worker.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Batch size is a design-time constant" | Hot tables and warehouse load shift nightly |
| "Airflow Variables are live config" | Workers read Variable at task start — not mid-loop |
| "We'll scale the Snowflake warehouse" | Credit burn spikes before resize completes |
| "One batch size for all tables" | Wide fact tables need smaller batches than dims |
| "Smaller batches always slower" | Lock waits make large batches slower tonight |

## The Aha

**`batch_row_count` is operational config** — it shifts during warehouse congestion, lock contention, and FinOps credit caps. It belongs in profile `['warehouse']['prod']['sync']` with local `get_int()` every batch iteration.

## What Kiponos.io is for warehouse sync batches

[Kiponos.io](https://kiponos.io) connects at Celery worker boot via `Kiponos.create_for_current_team()`. Profile `['warehouse']['prod']['sync']` hydrates in-process. Dashboard deltas update batch sizes; the **next** `fetch_batch()` reads locally.

`after_value_changed` logs batch shrinks and clears per-table cursor hints when `invalidate_cursors_on_change` is true.

Long-running sync loops pick up new batch sizes **without worker recycle**.

## Reference architecture

![Architecture diagram](https://files.catbox.moe/6y79qy.png)

## Config tree — sync, tables, throttle, warehouse, audit

Five folders — `sync`, `tables`, `throttle`, `warehouse`, `audit`:

```yaml
sync/
  default_batch_row_count: 5000
  min_batch_row_count: 200
  max_batch_row_count: 20000
  enabled: true
tables/
  fact_orders/
    batch_row_count: 5000
    priority: high
  dim_customers/
    batch_row_count: 5000
    priority: low
  fact_events/
    batch_row_count: 8000
    priority: medium
throttle/
  on_queue_depth: true
  queue_depth_threshold: 10
  shrunk_batch_row_count: 1200
warehouse/
  compute_wh_size: medium
  max_concurrent_batches: 4
audit/
  last_change_by: ""
  invalidate_cursors_on_change: false
```

Profile path: `['warehouse']['prod']['sync']`.

## Python integration: live batch sync + after_value_changed

```python
import logging
import os
from datetime import datetime

from kiponos import Kiponos

log = logging.getLogger(__name__)

os.environ.setdefault("KIPONOS_PROFILE", "['warehouse']['prod']['sync']")
kiponos = Kiponos.create_for_current_team()

_cursor_hints: dict[str, int] = {}


def _on_batch_change(change) -> None:
    if not str(change.path).startswith("tables/"):
        return
    log.info("Warehouse batch delta: path=%s value=%s", change.path, change.new_value)
    if kiponos.path("audit").get_bool("invalidate_cursors_on_change", False):
        _cursor_hints.clear()


kiponos.after_value_changed(_on_batch_change)


def resolve_batch_row_count(table: str) -> int:
    table_key = table.replace(".", "_")
    table_path = kiponos.path("tables", table_key)
    if table_path.exists():
        base = table_path.get_int("batch_row_count")
    else:
        base = kiponos.path("sync").get_int("default_batch_row_count", 5000)

    throttle = kiponos.path("throttle")
    if throttle.get_bool("on_queue_depth", False):
        depth = snowflake_monitor.queue_depth()
        if depth >= throttle.get_int("queue_depth_threshold", 10):
            return throttle.get_int("shrunk_batch_row_count", 1200)

    min_b = kiponos.path("sync").get_int("min_batch_row_count", 200)
    max_b = kiponos.path("sync").get_int("max_batch_row_count", 20000)
    return max(min_b, min(max_b, base))


def sync_table(cursor, table: str, watermark: datetime) -> int:
    if not kiponos.path("sync").get_bool("enabled", True):
        return 0

    total = 0
    while True:
        batch_size = resolve_batch_row_count(table)
        rows = cursor.execute(
            f"SELECT * FROM {table} WHERE updated_at > %s LIMIT %s",
            (watermark, batch_size),
        ).fetchall()

        if not rows:
            break

        warehouse_loader.upsert_batch(table, rows)
        watermark = max(r["updated_at"] for r in rows)
        total += len(rows)
        log.debug("Synced batch table=%s rows=%s batch_size=%s", table, len(rows), batch_size)

    return total
```

Every batch iteration re-reads `batch_row_count` from local memory — shrink takes effect on the **next** fetch, not next deploy.

## Real-world scenarios

| Scenario | Without live batch tree | With Kiponos DataOps sync |
|----------|------------------------|---------------------------|
| Snowflake queue depth 14 | Worker runs 5000-row batches | `tables/fact_orders/batch_row_count: 1200` live |
| Auto-throttle on congestion | Manual kill + redeploy | `throttle/on_queue_depth: true` |
| Dim tables unaffected | Risky global shrink | Per-table keys isolated |
| Warehouse recovers 04:00 | Edit Airflow for tomorrow | Dashboard restore to 5000 |
| FinOps credit review | Spreadsheet estimates | ACL shows who shrank batches |

## Performance: batch size reads in sync loop

- **One WebSocket per Celery worker** — not Snowflake API + Variable per batch
- **Batch resolve is 3 local reads** — microseconds vs warehouse query seconds
- **Delta patches** — shrink without killing in-flight worker
- **Throttle reads same tree** — auto-shrink coordinated with manual edits
- **Per-table isolation** — fact_orders shrink does not affect dim_customers

## Compare to alternatives

| Approach | Mid-sync shrink tonight | Per-table batches | Auto queue throttle |
|----------|------------------------|-------------------|----------------------|
| sync_config.py + redeploy | No — kills run | Awkward | No |
| Airflow Variable | Task restart only | Possible | No |
| Snowflake warehouse resize | Minutes + credits | N/A | Indirect |
| Hard-coded SQL LIMIT | Deploy required | No | No |
| **Kiponos SDK** | **Next batch iteration** | **Yes** | **Yes** |

## When not to use Kiponos for warehouse sync

| Boundary | Better home |
|----------|-------------|
| Snowflake warehouse size and credits | FinOps / cloud console |
| Table DDL and clustering keys | DBA GitOps |
| Source Postgres connection strings | Vault |
| Airflow DAG schedule and dependencies | Orchestrator repo |
| Data model schema design | Architecture docs |

## Getting started (15 minutes)

1. [Create TeamPro at kiponos.io](https://kiponos.io) — profile `['warehouse']['prod']['sync']`.
2. Add `kiponos` Python SDK to Celery worker environment.
3. Set `KIPONOS_ID`, `KIPONOS_ACCESS`, `KIPONOS_PROFILE=['warehouse']['prod']['sync']`.
4. Replace `BATCH_ROWS` with `resolve_batch_row_count(table)` in sync loop.
5. Register `after_value_changed` for audit logging.
6. Drill: staging — simulate queue depth and shrink `fact_orders/batch_row_count` — confirm next batch uses new size **without worker restart**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
- Related: [Spark shuffle partitions live](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-dataops-spark-shuffle-partitions-live.md)
- Related: [Schema evolution guardrails](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-dataops-schema-evolution-guardrails.md)

---

*Kiponos.io — Airflow schedules the run; batch_row_count lives in the tree.*