---
title: "REBUILD_INTERVAL_HOURS=24 Was Index Doctrine — We Partial-Refreshed Live During the Catalog Drop (Java)"
published: false
tags: java, springboot, ai, search
description: Vector index rebuild interval and partial refresh percent feel like infrastructure constants. When merchandising drops 40k SKUs, refresh policy is operational — Kiponos lets Java search services retune index cadence without restarts.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-ai-vector-index-refresh.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-ai-vector-index-refresh.jpg
---

Merchandising drop minute 8. Forty thousand new SKUs hit the product feed while your vector search still serves embeddings indexed **yesterday at 03:00** — because `REBUILD_INTERVAL_HOURS = 24` was "approved by search platform" eighteen months ago when the catalog changed weekly, not hourly.

Customer success forwards tickets: "Search returns discontinued items." P99 retrieval latency is fine. **Recall** is not.

The search tech lead says what every senior Java engineer has said:

> "Index rebuild interval is **infrastructure**. We do not change cron semantics without a platform review."

But the catalog event ends in four hours. A full rebuild takes six. You need a **partial refresh** of the hot partition **now**, and a shorter interval until the event passes — without recycling the JVM that holds the HNSW graph in memory.

Here is the Aha:

**`rebuild_interval_hours` and `partial_refresh_percent` behave like constants in `VectorIndexScheduler.java`, but they are dials you need during catalog spikes and embedding model rollouts.**

You can turn those dials **while the Spring Boot search service keeps serving queries** — no redeploy, no `@RefreshScope` bean recycle, no editing a CronJob manifest and waiting for Argo.

That is [Kiponos.io](https://kiponos.io).

## The problem: static refresh policy on the retrieval hot path

A typical Java vector search service schedules index maintenance like this:

```java
// VectorIndexScheduler.java
private static final int REBUILD_INTERVAL_HOURS = 24;
private static final int PARTIAL_REFRESH_PERCENT = 10;

@Scheduled(cron = "0 0 3 * * *")
public void nightlyRebuild() {
    if (!shouldRebuild()) return;
    indexService.fullRebuild();
}

@Scheduled(fixedDelayString = "${index.partial.delay:3600000}")
public void partialRefresh() {
    indexService.refreshPercent(PARTIAL_REFRESH_PERCENT);
}
```

Those values usually come from:

1. **YAML at startup** — change means rolling restart during peak shopping hours
2. **Database config poll** — adds JDBC load and latency when the scheduler tick runs
3. **Kubernetes CronJob in Git** — wrong latency for a four-hour merchandising flash event

The query path itself is fast — but **staleness** is a product incident. Operators need to change refresh policy **mid-event**.

| What teams say | What production does |
|----------------|---------------------|
| "Nightly rebuild is enough for our SLA" | Catalog drops do not read SLAs |
| "Partial refresh is risky — we prefer full rebuild" | Full rebuild during flash sale loses six hours of recall |
| "We'll fix it in next week's release" | Revenue walks to competitors with fresh search |

Teams **know** index freshness matters. They do not know there is a clean way to change refresh cadence **without recycling the JVM**.

## The Aha: index refresh policy is operational state

Wire `rebuild_interval_hours`, `partial_refresh_percent`, and event overrides into Kiponos. Your service still boots from minimal Spring config — but **live index policy** lives in the hub:

```yaml
vector_index/
  schedule/
    rebuild_interval_hours: 24
    partial_refresh_percent: 10
    partial_refresh_interval_min: 60
    full_rebuild_cron: "0 0 3 * * *"
  event_mode/
    enabled: false
    partial_refresh_percent: 40
    partial_refresh_interval_min: 15
    pause_full_rebuild: true
  partitions/
    hot_partition_ids: electronics,seasonal
    cold_refresh_percent: 5
```

During the catalog drop, ops enables `event_mode` and sets `partial_refresh_percent` to `40`. WebSocket delivers a **delta**. The next scheduler tick reads `40` and refreshes four times more of the hot partition per cycle. **No restart.**

## What Kiponos.io is — for Java vector search services

[Kiponos.io](https://kiponos.io) is a real-time config hub with **Java** (Spring Boot 2/3) and **Python** SDKs. `Kiponos.createForCurrentTeam()` connects over WebSocket, hydrates the tree for a profile like `['search']['vector']['prod']['index']`, and serves **local** `getInt()` / `getBool()` on scheduler ticks and query routing.

Updates are **async deltas** — changing `partial_refresh_percent` patches one key in memory. Your `@Scheduled` methods never block on HTTP config calls.

No restart. No redeploy. No per-query remote fetch. Profile path: `['app']['release']['env']['config']`.

`afterValueChanged` can trigger an immediate partial refresh when ops flips `event_mode` — binder runs off the request path.

## Architecture

![Architecture diagram](https://litter.catbox.moe/m64rbm.png)

1. **Connect once** at Spring Boot startup.
2. **Full tree snapshot** for index profile.
3. **Dashboard edit** sends **delta only**.
4. **SDK merges async** on WebSocket thread.
5. **Reads are local** — scheduler and query path never wait on network.

## Example config tree

```yaml
vector_index/
  schedule/
    rebuild_interval_hours: 24
    partial_refresh_percent: 10
    partial_refresh_interval_min: 60
    full_rebuild_cron: "0 0 3 * * *"
    max_concurrent_refresh_jobs: 2
  event_mode/
    enabled: false
    partial_refresh_percent: 40
    partial_refresh_interval_min: 15
    pause_full_rebuild: true
  partitions/
    hot_partition_ids: electronics,seasonal
    cold_refresh_percent: 5
    prefer_hot_on_query: true
  quality/
    min_recall_threshold: 0.85
    alert_on_staleness_min: 120
```

## Java integration (Spring Boot vector search)

```java
import io.kiponos.sdk.Kiponos;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class VectorIndexScheduler {

    private final Kiponos kiponos;
    private final VectorIndexService indexService;

    public VectorIndexScheduler(Kiponos kiponos, VectorIndexService indexService) {
        this.kiponos = kiponos;
        this.indexService = indexService;
        kiponos.afterValueChanged(change -> {
            if (change.path().startsWith("vector_index/event_mode")
                    && kiponos.path("vector_index", "event_mode").getBool("enabled", false)) {
                runPartialRefreshNow();
            }
        });
    }

    private IndexPolicy currentPolicy() {
        var schedule = kiponos.path("vector_index", "schedule");
        var event = kiponos.path("vector_index", "event_mode");
        int percent = schedule.getInt("partial_refresh_percent", 10);
        int intervalMin = schedule.getInt("partial_refresh_interval_min", 60);
        boolean pauseFull = false;
        if (event.getBool("enabled", false)) {
            percent = event.getInt("partial_refresh_percent", 40);
            intervalMin = event.getInt("partial_refresh_interval_min", 15);
            pauseFull = event.getBool("pause_full_rebuild", true);
        }
        return new IndexPolicy(percent, intervalMin, pauseFull);
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void nightlyRebuild() {
        IndexPolicy policy = currentPolicy();  // local memory read
        if (policy.pauseFullRebuild()) return;
        indexService.fullRebuild();
    }

    @Scheduled(fixedDelay = 60_000)
    public void partialRefresh() {
        IndexPolicy policy = currentPolicy();
        indexService.refreshPercent(policy.partialRefreshPercent());
    }

    public void runPartialRefreshNow() {
        IndexPolicy policy = currentPolicy();
        indexService.refreshPercent(policy.partialRefreshPercent());
    }
}
```

Hot-path query routing can read the same tree:

```java
@RestController
public class SearchController {
    private final Kiponos kiponos;
    private final VectorSearchService searchService;

    @GetMapping("/search")
    public SearchResponse search(@RequestParam String q) {
        boolean preferHot = kiponos.path("vector_index", "partitions")
                .getBool("prefer_hot_on_query", true);
        return searchService.query(q, preferHot);
    }
}
```

Every `getInt()` is a **local memory read** — microseconds, not milliseconds.

## Real scenarios

| Event | Frozen `REBUILD_INTERVAL_HOURS=24` | Kiponos path |
|-------|-------------------------------------|--------------|
| 40k SKU merchandising drop | Stale recall until tomorrow 03:00 | Enable `event_mode`, `partial_refresh_percent: 40` live |
| Embedding model v2 rollout | Full rebuild blocks partial refresh | `pause_full_rebuild: true`, raise partial percent |
| Post-event cooldown | Manual CronJob edit in Git | Disable `event_mode`, restore `schedule` defaults from dashboard |
| Black Friday prep | Three config branches | Same JAR, hub profile `event/black-friday` |

## Performance — why search stays fast

- One WebSocket per JVM — not one config fetch per scheduler tick
- `getInt("partial_refresh_percent")` is O(1) on cached tree
- Delta updates — event mode toggle sends handful of keys, not full redeploy
- Index rebuild runs on scheduler thread, not per query
- `afterValueChanged` triggers optional immediate refresh — still off HTTP request path

## Compare to alternatives

| Approach | Raise partial refresh during catalog drop | Read cost on scheduler tick |
|----------|-------------------------------------------|-----------------------------|
| `application-prod.yml` constants | PR + deploy (25+ min) | Zero until restart |
| `@RefreshScope` + actuator | Context refresh risk | Bean recycle |
| Poll Redis for index policy | Fast dashboard | Network RTT per tick |
| Edit Kubernetes CronJob | GitOps latency | N/A — wrong abstraction |
| **Kiponos SDK** | **Dashboard (seconds)** | **Memory read** |

## When not to use Kiponos for vector index refresh

| Case | Better approach |
|------|-----------------|
| Switching vector DB vendor (Pinecone → pgvector) | Architecture migration in Git |
| HNSW `efConstruction` / `M` graph params | Code change + full rebuild |
| Embedding model dimension change | New index version + cutover |
| Storing vectors themselves | Object store / DB — not runtime hub |

## Getting started (15 minutes)

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['search']['vector']['prod']['index']`.
2. Add `io.kiponos:sdk-boot-3` — wire `KIPONOS_ID`, `KIPONOS_ACCESS`, profile path.
3. Create `vector_index/schedule` and `vector_index/event_mode` with refresh keys.
4. Replace `static final` refresh constants with `currentPolicy()` using `kiponos.path(...)`.
5. Game day: inject synthetic catalog delta in staging, enable `event_mode` live, watch partial refresh cadence accelerate **without pod restart**.

**Further reading:**

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — index refresh interval is live operational state, not cron doctrine.*