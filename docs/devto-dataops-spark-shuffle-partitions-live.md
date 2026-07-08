---
title: "Spark Shuffle Partitions Live — Right-Size Jobs Mid-Pipeline (Python SDK)"
published: false
tags: python, spark, data, architecture
description: shuffle.partitions in spark-submit is batch-static. Kiponos holds partition counts ops tunes between nightly runs.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-dataops-spark-shuffle-partitions-live.md
main_image: https://files.catbox.moe/e166i8.jpg
---

Tuesday 01:52 UTC. The **nightly customer-360 ETL** shows severe **shuffle skew** — one partition holds 41% of rows after a marketing segment join exploded. The DAG still submits with `SHUFFLE_PARTITIONS = 200` from `spark_defaults.py`, chosen when the dimension table was one-tenth its current size.

The data engineer on bridge wants `shuffle_partitions` at **480** for tonight's rerun only — not a permanent `spark-submit` arg change merged through review. Platform ops asks:

> "Partition count is an **ops knob for this job hour** — not a **cluster redesign**. Why can't we bump it before the 02:00 Airflow trigger without editing the DAG repo?"

Most Python Spark pipelines encode shuffle policy as **spark-submit args**, **Airflow Variables**, and **module constants** — none hot-updatable between runs. [Kiponos.io](https://kiponos.io) holds per-job partition counts in profile `['etl']['prod']['spark']` with **local `get_int()` before each stage**.

## The problem: shuffle_partitions frozen at submit time

```python
# spark_defaults.py — imported at driver boot
SHUFFLE_PARTITIONS = 200

def build_customer_360(spark: SparkSession) -> None:
    spark.conf.set("spark.sql.shuffle.partitions", str(SHUFFLE_PARTITIONS))
    segments = spark.table("dim_marketing_segment")
    events = spark.table("fact_customer_events")
    joined = events.join(segments, "segment_id", "inner")
    joined.write.mode("overwrite").saveAsTable("mart.customer_360")
```

Airflow passes static args:

```bash
spark-submit --conf spark.sql.shuffle.partitions=200 customer_360.py
```

During skew you need to:

1. Raise **`jobs.customer_360.shuffle_partitions`** to 480 tonight
2. Keep **`jobs.daily_aggregates.shuffle_partitions`** at 200
3. Enable **`skew_guard.auto_bump_on_detect`** for future runs

Editing the DAG repo at 01:55 misses the 02:00 slot.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "shuffle.partitions is a cluster constant" | Optimal count shifts with daily data volume |
| "We'll fix skew in the next sprint" | Tonight's SLA misses before PR merges |
| "Airflow Variables are dynamic enough" | Driver still reads Variable at parse time only |
| "Adaptive Query Execution fixes it" | AQE helps — but baseline partition count still matters |
| "One partition count for all jobs" | Customer-360 and aggregates have different shapes |

## The Aha

**`shuffle_partitions` is operational config** — it changes during skew incidents, volume spikes, and FinOps right-sizing. It belongs in profile `['etl']['prod']['spark']` with local `get_int()` before each job stage.

## What Kiponos.io is for Spark shuffle tuning

[Kiponos.io](https://kiponos.io) connects once at Spark driver boot via `Kiponos.create_for_current_team()`. Profile `['etl']['prod']['spark']` hydrates in-process. Dashboard edits patch **deltas**; the next `apply_shuffle_config()` reads new integers locally.

`after_value_changed` logs partition bumps and optionally invalidates cached stage plans when `invalidate_on_change` is true.

No driver restart mid-run for **subsequent** triggered jobs. Same driver can read updated count on retry trigger.

## Reference architecture

![Architecture diagram](https://files.catbox.moe/jvroyn.png)

## Config tree — spark, jobs, skew_guard, aqe, audit

Five folders — `spark`, `jobs`, `skew_guard`, `aqe`, `audit`:

```yaml
spark/
  default_shuffle_partitions: 200
  min_shuffle_partitions: 50
  max_shuffle_partitions: 2000
  enabled: true
jobs/
  customer_360/
    shuffle_partitions: 200
    coalesce_before_write: false
  daily_aggregates/
    shuffle_partitions: 120
    coalesce_before_write: true
  fraud_features/
    shuffle_partitions: 400
    coalesce_before_write: false
skew_guard/
  auto_bump_on_detect: false
  bump_multiplier: 2.4
  detect_skew_pct_threshold: 25
aqe/
  enabled: true
  advisory_shuffle_partitions: true
audit/
  last_change_by: ""
  invalidate_on_change: true
```

Profile path: `['etl']['prod']['spark']`.

## Python integration: live shuffle config + after_value_changed

```python
import logging
import os

from kiponos import Kiponos
from pyspark.sql import SparkSession

log = logging.getLogger(__name__)

os.environ.setdefault("KIPONOS_PROFILE", "['etl']['prod']['spark']")
kiponos = Kiponos.create_for_current_team()

_stage_plan_cache: dict[str, int] = {}


def _on_shuffle_change(change) -> None:
    if not str(change.path).startswith("jobs/"):
        return
    if kiponos.path("audit").get_bool("invalidate_on_change", True):
        _stage_plan_cache.clear()
        log.info("Cleared stage plan cache after shuffle change: %s", change.path)
    log.info("Shuffle partition delta: path=%s value=%s", change.path, change.new_value)


kiponos.after_value_changed(_on_shuffle_change)


def resolve_shuffle_partitions(job_name: str) -> int:
    job_path = kiponos.path("jobs", job_name)
    if job_path.exists():
        return job_path.get_int("shuffle_partitions")

    skew = kiponos.path("skew_guard")
    base = kiponos.path("spark").get_int("default_shuffle_partitions", 200)
    if skew.get_bool("auto_bump_on_detect", False) and skew_detector.is_hot(job_name):
        mult = skew.get_float("bump_multiplier", 2.0)
        max_p = kiponos.path("spark").get_int("max_shuffle_partitions", 2000)
        return min(max_p, int(base * mult))
    return base


def apply_shuffle_config(spark: SparkSession, job_name: str) -> int:
    partitions = resolve_shuffle_partitions(job_name)
    spark.conf.set("spark.sql.shuffle.partitions", str(partitions))

    aqe = kiponos.path("aqe")
    if aqe.get_bool("enabled", True):
        spark.conf.set("spark.sql.adaptive.enabled", "true")
        if aqe.get_bool("advisory_shuffle_partitions", True):
            spark.conf.set("spark.sql.adaptive.coalescePartitions.enabled", "true")

    _stage_plan_cache[job_name] = partitions
    log.info("Applied shuffle_partitions=%s for job=%s", partitions, job_name)
    return partitions


def build_customer_360(spark: SparkSession) -> None:
    apply_shuffle_config(spark, "customer_360")
    segments = spark.table("dim_marketing_segment")
    events = spark.table("fact_customer_events")
    joined = events.join(segments, "segment_id", "inner")
    if kiponos.path("jobs", "customer_360").get_bool("coalesce_before_write", False):
        joined = joined.coalesce(resolve_shuffle_partitions("customer_360") // 4)
    joined.write.mode("overwrite").saveAsTable("mart.customer_360")
```

Every `get_int()` before stage planning is **local memory** — no HTTP during driver initialization.

## Real-world scenarios

| Scenario | Without live shuffle tree | With Kiponos DataOps partitions |
|----------|--------------------------|--------------------------------|
| Tonight's skew on customer-360 | Edit DAG; miss 02:00 slot | `jobs/customer_360/shuffle_partitions: 480` |
| Daily aggregates unchanged | Risky global spark.conf change | Per-job keys isolated |
| Detected skew next week | Manual rerun with new args | `skew_guard/auto_bump_on_detect: true` |
| Post-incident restore | Revert DAG commit | Dashboard reset to 200 |
| FinOps asks who bumped partitions | Git blame on constants.py | Kiponos ACL + change log |

## Performance: shuffle partition reads at driver boot

- **One WebSocket per Spark driver** — not Airflow API + Variable fetch per stage
- **Partition resolve is 2–3 local reads** — microseconds vs cluster negotiate
- **Delta patches** — one job key without resubmitting cluster defaults
- **`after_value_changed` clears stale plans** — next triggered run picks up bump
- **AQE flags coexist** in same tree — coordinated tuning posture

## Compare to alternatives

| Approach | Tonight's skew bump | Per-job isolation | Skew auto-bump |
|----------|--------------------|--------------------|----------------|
| spark-submit args | DAG edit + redeploy | Awkward | No |
| Airflow Variable | Parse-time only | Possible | No |
| Databricks cluster policy | Cluster-wide | Partial | Vendor UI |
| EMR step args | Step resubmit | Per-step | No |
| **Kiponos SDK** | **Dashboard seconds** | **Yes** | **Yes** |

## When not to use Kiponos for Spark shuffle

| Boundary | Better home |
|----------|-------------|
| Executor instance types and autoscaling | Cloud console / Terraform |
| Iceberg/Delta table retention and compaction | Table format ops |
| Spark version upgrades | Platform GitOps |
| JDBC connection strings and warehouse passwords | Vault |
| Physical shuffle disk sizing | Cluster infrastructure |

## Getting started (15 minutes)

1. [Create TeamPro at kiponos.io](https://kiponos.io) — profile `['etl']['prod']['spark']`.
2. Add `kiponos` Python SDK to your Spark driver environment.
3. Set `KIPONOS_ID`, `KIPONOS_ACCESS`, `KIPONOS_PROFILE=['etl']['prod']['spark']`.
4. Call `apply_shuffle_config(spark, job_name)` at job start instead of constants.
5. Register `after_value_changed` for stage plan cache invalidation.
6. Drill: staging — bump `customer_360/shuffle_partitions` and rerun — confirm Spark UI shows new partition count **without editing DAG**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
- Related: [Warehouse sync batch sizes](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-dataops-warehouse-sync-batch-sizes.md)
- Related: [Cost control runtime](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-cost-control-runtime.md)

---

*Kiponos.io — spark-submit bootstraps the cluster; shuffle_partitions lives in the tree.*