---
title: "scroll.timeout=1m Was Indexing Dogma — We Extended It Live During the Reindex Marathon (Elasticsearch Java Client)"
published: false
tags: java, elasticsearch, search, devops
description: Elasticsearch scroll timeout feels like index policy set once in query code. When reindex jobs hit slow shards, scroll keepalive is operational — Kiponos feeds live search batch policy without job restart.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-elasticsearch-scroll-timeout.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-aha-elasticsearch-scroll-timeout.jpg
---

Catalog reindex job hour 3. The nightly `products_v2` migration is 62% complete when shard recovery on node `es-data-07` slows bulk indexing. Scroll contexts start expiring — `search_phase_execution_exception: search context missing` — because every batch uses `Time.of(t -> t.time("1m"))` copied from the original reindex playbook when batches finished in twenty seconds.

SRE pings the search team. The response is familiar:

> "One minute scroll timeout is **Elasticsearch best practice**. We do not extend keepalive without index governance review."

But the job dies at 62% and tomorrow's merchandising launch does not care about governance calendars. Scroll timeout is not dogma — it is **how long you ask the cluster to remember your cursor between slow batches**.

**The Aha:** read `scroll_timeout_sec` from [Kiponos.io](https://kiponos.io) on every scroll request — ops sets `300` live while the reindex worker keeps running.

## The problem: scroll keepalive frozen in repository code

```java
public class ProductReindexJob {

    private static final String SCROLL_TIMEOUT = "1m";
    private static final int BATCH_SIZE = 500;

    public void run() {
        var response = client.search(s -> s
                .index("products_v1")
                .size(BATCH_SIZE)
                .scroll(Time.of(t -> t.time(SCROLL_TIMEOUT))),
                ProductDoc.class);

        while (response.hits().hits().size() > 0) {
            bulkIndexToV2(response.hits().hits());
            response = client.scroll(s -> s
                    .scrollId(response.scrollId())
                    .scroll(Time.of(t -> t.time(SCROLL_TIMEOUT))),
                    ProductDoc.class);
        }
    }
}
```

Problems when bulk indexing slows:

1. **Context expiry mid-job** — restart from scratch or complex checkpointing
2. **Deploy to extend** — kills running scroll id chain
3. **Too long forever** — after recovery, open scroll contexts waste heap unless someone reverts

| What teams say | What production does |
|----------------|---------------------|
| "1m is ES official guidance" | Guidance assumes fast batches; yours are not tonight |
| "Longer scroll hurts cluster memory" | Expired contexts **fail the job** — worse than controlled extension |
| "Fix shard recovery, don't touch scroll" | Merchandising needs progress **now** |
| "Reindex params belong in job code" | Scroll keepalive is operational patience |

## What is Kiponos.io — for Elasticsearch batch policy

[Kiponos.io](https://kiponos.io) stores operational search knobs under profile `['catalog']['prod']['elasticsearch']`. WebSocket deltas patch the in-memory tree. `getInt("scroll_timeout_sec")` is a **local read** when building each scroll call — no HTTP config fetch between batches.

Git keeps **index names and mapping versions**; the hub keeps **scroll seconds this reindex run**.

## Architecture

![Architecture diagram](https://files.catbox.moe/essvkb.png)

## Config tree

```yaml
elasticsearch/
  reindex/
    products/
      scroll_timeout_sec: 60
      batch_size: 500
      enabled: true
      slow_shard_warn_ms: 5000
    customers/
      scroll_timeout_sec: 120
      batch_size: 1000
  ops/
    recovery_mode: false
    recovery_scroll_timeout_sec: 300
    reduce_batch_size: false
    recovery_batch_size: 200
  cluster/
    request_timeout_ms: 30000
```

## Integration (Spring Boot Elasticsearch client)

```java
@Configuration
public class KiponosConfig {

    @Bean
    public Kiponos kiponos(
            @Value("${kiponos.team-id}") String teamId,
            @Value("${kiponos.access-key}") String accessKey,
            @Value("${kiponos.profile-path}") String profilePath) {
        return Kiponos.builder()
                .teamId(teamId)
                .accessKey(accessKey)
                .profilePath(profilePath)
                .build();
    }
}
```

```java
@Component
public class ProductReindexJob {

    private final Kiponos kiponos;
    private final ElasticsearchClient client;

    public ProductReindexJob(Kiponos kiponos, ElasticsearchClient client) {
        this.kiponos = kiponos;
        this.client = client;
        kiponos.afterValueChanged(change -> {
            if (change.path().startsWith("elasticsearch/reindex/products")
                    || change.path().startsWith("elasticsearch/ops")) {
                log.warn("ES reindex policy: {} → {}", change.path(), change.newValue());
            }
        });
    }

    public void run() {
        var cfg = kiponos.path("elasticsearch", "reindex", "products");
        if (!cfg.getBool("enabled", true)) return;

        String scrollTime = scrollTimeSpec();
        int batchSize = resolveBatchSize(cfg);
        long batchStart = System.nanoTime();

        var response = client.search(s -> s
                .index("products_v1")
                .size(batchSize)
                .scroll(Time.of(t -> t.time(scrollTime))),
                ProductDoc.class);

        while (!response.hits().hits().isEmpty()) {
            bulkIndexToV2(response.hits().hits());
            long elapsedMs = (System.nanoTime() - batchStart) / 1_000_000;
            if (elapsedMs > cfg.getLong("slow_shard_warn_ms", 5000)) {
                log.warn("slow reindex batch: {}ms (scroll={})", elapsedMs, scrollTime);
            }
            batchStart = System.nanoTime();
            scrollTime = scrollTimeSpec(); // pick up live changes between batches
            String nextScroll = scrollTime;
            response = client.scroll(s -> s
                    .scrollId(response.scrollId())
                    .scroll(Time.of(t -> t.time(nextScroll))),
                    ProductDoc.class);
        }
    }

    private String scrollTimeSpec() {
        int sec = kiponos.path("elasticsearch", "ops").getBool("recovery_mode", false)
                ? kiponos.path("elasticsearch", "ops").getInt("recovery_scroll_timeout_sec", 300)
                : kiponos.path("elasticsearch", "reindex", "products").getInt("scroll_timeout_sec", 60);
        return sec + "s";
    }

    private int resolveBatchSize(ConfigPath cfg) {
        if (kiponos.path("elasticsearch", "ops").getBool("reduce_batch_size", false)) {
            return kiponos.path("elasticsearch", "ops").getInt("recovery_batch_size", 200);
        }
        return cfg.getInt("batch_size", 500);
    }
}
```

Shard recovery dragging? Ops enables `recovery_mode` and `recovery_scroll_timeout_sec: 300`. **Next scroll requests** use five-minute keepalive — job continues without JVM restart.

## Real scenarios

| Event | `SCROLL_TIMEOUT = "1m"` dogma | Kiponos path |
|-------|-------------------------------|--------------|
| Slow shard recovery | Job fails at 62%; restart from zero | `recovery_mode: true` live |
| Cluster healthy again | Still holding 5m contexts until deploy | Disable recovery mode |
| Merchandising launch week | Emergency branch per job | Hub profile `reindex/patient` |
| Audit "who extended scroll?" | Git blame on constant | Dashboard audit on `elasticsearch/ops` |

## Performance — why reindex stays efficient

- **`getInt()` once per scroll batch** — not per document in bulk payload
- **One WebSocket** per reindex worker JVM
- **Re-read scroll timeout between batches** — live change without restarting scroll id chain from scratch
- **Optional smaller `batch_size` in recovery** — pairs with longer scroll for stability
- **Delta updates** — recovery mode toggles three keys, instant merge

## Compare to alternatives

| Approach | Extend scroll during slow shards | Per-batch overhead |
|----------|----------------------------------|-------------------|
| Hard-coded `"1m"` | Redeploy job; lose scroll context | Zero (frozen) |
| Job CLI args | Restart worker with new args | Shell access + downtime |
| Poll Redis per batch | Possible | RTT every scroll |
| **Kiponos SDK** | **Dashboard (seconds)** | **Memory read** |

## When not to use Kiponos for Elasticsearch scroll

| Case | Better approach |
|------|-----------------|
| Index mapping and analyzer definitions | Git + reviewed migrations |
| Cluster node count and shard allocation | Infrastructure GitOps |
| Switching scroll → search_after pagination redesign | Architecture change |
| `scroll_timeout_sec: 3600` without heap math | Cluster capacity review first |

## Getting started (15 minutes)

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['catalog']['prod']['elasticsearch']`.
2. Add `io.kiponos:sdk-boot-3` to your reindex worker service.
3. Create `elasticsearch/reindex/products` with scroll and recovery keys.
4. Replace `static final SCROLL_TIMEOUT` with `scrollTimeSpec()` local read.
5. Staging: throttle bulk indexing, enable `recovery_mode`, confirm job completes **without worker restart**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — scroll timeout is how long the cluster holds your place, not indexing dogma from the playbook.*