---
title: "Retention Purge Schedules Live — Adjust Windows Without Batch Redeploy (Java SDK)"
published: false
tags: java, compliance, data, architecture
description: Purge job schedules in cron YAML are slow to change. Kiponos holds retention windows for ops — legal retention policies stay in wiki.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-regops-retention-purge-schedules-live.md
main_image: https://files.catbox.moe/574r3o.jpg
---

Tuesday 19:44 UTC. A **storage incident** on the analytics lake triggers an emergency hold — hot-tier data must stay queryable for fourteen extra days while forensic copies complete. The nightly purge job still deletes partitions older than `PURGE_AFTER_DAYS = 90` because `data-retention.yml` was signed off in Q1.

The data platform on-call needs `purge_after_days` bumped to 104 **tonight** — not after tomorrow's batch redeploy window. Legal confirms the **legal retention minimum** is unchanged; this is **temporary ops extension** of the hot window. The DBA asks:

> "Why does **extending the purge cutoff** require **recycling purge workers** when only one integer moved?"

**Honest framing:** Kiponos lets ops tune **`purge_after_days`** and related purge guards while schedulers run. This is **operational retention window control** — not legal advice, not replacement for counsel-approved retention schedules, and not WORM immutability design. Legal minimums stay documented; the tree holds **what purge jobs enforce tonight**.

## The problem: purge_after_days frozen in batch workers

```java
@Scheduled(cron = "0 2 * * *")
public class PartitionPurgeJob {
    private static final int PURGE_AFTER_DAYS = 90;

    public void purgeExpiredPartitions() {
        LocalDate cutoff = LocalDate.now().minusDays(PURGE_AFTER_DAYS);
        lakehouse.dropPartitionsOlderThan(cutoff);
    }
}
```

Static YAML:

```yaml
data:
  prod:
    retention:
      purge_after_days: 90
```

During a storage incident you need to:

1. Raise **`purge_after_days`** to 104 temporarily
2. Set **`hold.forensic_copy_in_progress: true`** to skip destructive steps
3. Lower **`batch.max_partitions_per_run`** to reduce I/O blast radius

Redeploying purge workers mid-incident risks **double-delete** or **missed hold** if old and new constants race.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Retention days are legal constants" | Legal sets minimums; ops extends hot windows during incidents |
| "We'll pause the cron in Airflow" | JVM workers still read boot constants |
| "Hold flags belong in tickets" | Tickets do not gate `dropPartitionsOlderThan()` |
| "Forensic copy is a manual DBA step" | Purge jobs need live coordination with copy progress |
| "Staging purge matches prod" | Hold keys never seeded in lower envs |

## The Aha

**`purge_after_days` is operational config** — it shifts during storage incidents, forensic holds, and FinOps tiering changes. It belongs in profile `['data']['prod']['retention']` with local `getInt()` on every purge evaluation.

## What Kiponos.io is for retention purge (RegOps)

[Kiponos.io](https://kiponos.io) hydrates `['data']['prod']['retention']` into purge scheduler JVMs. Dashboard edits propagate via WebSocket deltas.

`afterValueChanged` logs retention window changes and notifies data platform — **without** recycling batch workers.

**RegOps boundary:** Kiponos ACL records **who extended purge windows** — useful operational evidence alongside legal retention docs. It does **not** certify regulatory retention compliance.

## Reference architecture

![Architecture diagram](https://files.catbox.moe/9aid5m.png)

## Config tree — retention, hold, batch, tiers, meta

Five folders — `retention`, `hold`, `batch`, `tiers`, `meta`:

```yaml
retention/
  purge_after_days: 90
  min_purge_after_days: 30
  max_purge_after_days: 365
  enabled: true
hold/
  forensic_copy_in_progress: false
  skip_destructive_purge: true
  hold_expires_at_ms: 0
batch/
  max_partitions_per_run: 500
  throttle_on_cluster_load_pct: 85
  pause_when_load_above_pct: 95
tiers/
  hot_days: 90
  warm_days: 365
  cold_archive_days: 2555
meta/
  last_change_by: ""
  siem_forward_enabled: true
```

Profile path: `['data']['prod']['retention']`.

## Java integration: live purge scheduler + afterValueChanged

```java
import io.kiponos.sdk.Kiponos;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class RegOpsPurgeScheduler {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();
    private final LakehouseClient lakehouse;

    public RegOpsPurgeScheduler(LakehouseClient lakehouse) {
        this.lakehouse = lakehouse;
        kiponos.afterValueChanged(change -> {
            log.info("Retention purge delta: path={} value={}", change.path(), change.newValue());
            if (kiponos.path("meta").getBool("siem_forward_enabled")) {
                siemClient.emit("regops_retention_change", change.path(), change.newValue());
            }
        });
    }

    @Scheduled(cron = "0 2 * * *")
    public void purgeExpiredPartitions() {
        if (!kiponos.path("retention").getBool("enabled")) {
            return;
        }

        var hold = kiponos.path("hold");
        if (hold.getBool("forensic_copy_in_progress")
            && hold.getBool("skip_destructive_purge")) {
            log.info("Skipping purge — forensic hold active");
            return;
        }

        if (clusterLoadMonitor.currentPct()
            >= kiponos.path("batch").getInt("pause_when_load_above_pct")) {
            log.warn("Skipping purge — cluster load too high");
            return;
        }

        int days = kiponos.path("retention").getInt("purge_after_days");
        LocalDate cutoff = LocalDate.now().minusDays(days);
        int maxParts = kiponos.path("batch").getInt("max_partitions_per_run");

        lakehouse.dropPartitionsOlderThan(cutoff, maxParts);
    }
}
```

## Real-world scenarios

| Scenario | Without live retention tree | With Kiponos RegOps purge |
|----------|----------------------------|---------------------------|
| Storage incident hold | Redeploy purge workers | `purge_after_days: 104` live |
| Forensic copy in flight | Risk of premature delete | `hold/forensic_copy_in_progress: true` |
| Cluster load spike | Full purge hammers IO | `batch/pause_when_load_above_pct` gates job |
| Post-incident restore | Second deploy | Reset days in dashboard |
| Auditor asks who extended window | Tickets + deploy logs | Kiponos ACL + SIEM |

## Performance: purge policy reads in batch jobs

- **One WebSocket per scheduler JVM** — not JDBC per partition scan
- **Purge evaluation is ≤5 local reads** — microseconds vs lakehouse I/O
- **Delta patches** — one integer change without worker restart
- **Hold flags apply on next cron tick** — no missed midnight window from deploy lag
- **Batch throttle keys** coexist with purge days in one tree

## Compare to alternatives

| Approach | Tonight's hold extension | Worker restart | Hold + batch throttle together |
|----------|-------------------------|----------------|-------------------------------|
| data-retention.yml + redeploy | No — deploy window | Required | Partial |
| Airflow variable only | Orchestrator knows; JVM stale | Partial | Split brain |
| Database policy table | Yes with JDBC | No | Possible |
| **Kiponos SDK** | **Seconds** | **None** | **Yes** |

## When not to use Kiponos for retention purge

| Boundary | Better home |
|----------|-------------|
| Legal minimum retention periods | Compliance wiki + counsel |
| Immutable WORM archive design | Object Lock / tape policy |
| Whether extension satisfies GDPR/SOX | Compliance officer — not this article |
| Lakehouse physical tier migration | Infra / DBA tooling |
| Encryption keys for archived data | Vault / KMS |

## Getting started (15 minutes)

1. [Create TeamPro at kiponos.io](https://kiponos.io) — profile `['data']['prod']['retention']`.
2. Add `io.kiponos:sdk-boot-3` to purge scheduler service.
3. Set `-Dkiponos="['data']['prod']['retention']"`.
4. Replace `PURGE_AFTER_DAYS` with `kiponos.path("retention").getInt("purge_after_days")`.
5. Wire `afterValueChanged` and hold flags.
6. Drill: staging — enable forensic hold and confirm purge skips **without worker restart**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
- Related: [Audit log sampling rates live](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-regops-audit-log-sampling-rates.md)
- Related: [Cost control runtime](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-cost-control-runtime.md)

---

*Kiponos.io — legal retention prose lives in the wiki; purge_after_days lives in the tree.*