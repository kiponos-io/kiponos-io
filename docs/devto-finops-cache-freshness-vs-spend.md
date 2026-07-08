---
title: "Cache Freshness vs Spend — Live TTL Tradeoffs Without a Deploy (Java SDK)"
published: false
tags: architecture, finops, java, python
description: TTL spreadsheets vs Redis constants split FinOps intent from enforcement. Kiponos holds freshness tiers and cost-aware TTL ladders in one operational tree.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-finops-cache-freshness-vs-spend.md
main_image: https://files.catbox.moe/n1iaiq.jpg
---

Tuesday 11:03 UTC. FinOps posts in `#catalog-platform`: **CDN egress** for **catalog-api** is **280% above forecast** — image variants and product JSON are re-fetching every **60 seconds** because marketing launched a flash sale with aggressive cache-busting headers. Edge bill is climbing; Postgres is actually fine.

Cache platform lead **Elena Vasquez** pings SRE **Jonah Park**. Jonah does not want to shorten TTL globally — PDP freshness matters for price accuracy. He needs to **lengthen TTL on non-critical paths only** while keeping search and inventory hot:

> "Bump `default_ttl_sec` on browse and category paths to **600**. Leave search at **30**. I am not opening a PR while Akamai invoices us by the gigabyte."

The catalog service still reads `CACHE_TTL_SECONDS = 300` from `CachePolicy.java`, a single integer from the Q2 cost review. The CDN team maintains a spreadsheet of "recommended TTLs by path class" that nobody wired into code. Redis has per-key TTLs set at write time from that constant.

The FinOps owner asks on Slack:

> "We already choose TTL on every cache write. Why does **freshness vs spend** require a **deploy** when the knob is seconds?"

Most Java catalog services treat TTL as **engineering constant**: one `static final`, a CDN runbook PDF, and billing alerts that fire after egress already spiked. [Kiponos.io](https://kiponos.io) collapses default TTL ladders, path-class overrides, and cost-posture flags into **one operational tree** — readable on every cache operation with local `get*()` calls and adjustable from the dashboard while JVMs keep running.

## The problem — default_ttl_sec baked into static config

A typical catalog edge service sets TTL like this:

```java
@Service
public class CatalogCacheWriter {
    private static final int CACHE_TTL_SECONDS = 300;

    public void put(String key, byte[] payload, PathClass pathClass) {
        redis.setex(key, CACHE_TTL_SECONDS, payload);
        cdn.purgeOnWrite(key);  // aggressive — egress multiplies on flash sales
    }
}
```

TTL policy usually lives elsewhere — scattered and deploy-bound:

```yaml
# application-prod.yml — requires restart to change
catalog:
  cache:
    default-ttl-sec: 300
    search-ttl-sec: 30
    category-ttl-sec: 600
```

Or worse — one global TTL because path-specific env vars never shipped:

```java
// "We'll add path classes in v2"
private static final int CACHE_TTL_SECONDS = 300;
```

The catalog path executes **thousands of cache writes per second** during promotions. During a CDN bill spike you need to:

1. Raise **`paths/browse/default_ttl_sec`** to cut origin fetches without freezing prices on PDP
2. Keep **`paths/search/default_ttl_sec`** low so inventory-sensitive queries stay fresh
3. Flip **`posture/aggressive_purge_disabled`** to stop purge storms that multiply egress

Doing that through a deploy while Akamai meters every gigabyte is not FinOps — it is **invoice theater with compound interest**.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "CDN TTL is configured in the portal" | App-layer TTL on cache write often **overrides** edge intent |
| "Shorter TTL always means fresher catalog" | Purge-on-write plus short TTL **multiplies** origin and egress cost |
| "We'll alert at 80% CDN budget" | Alerts inform humans; cache writes keep using stale constants |
| "Redis TTL is set once per key — can't change mid-flight" | New writes pick up live policy; you need the policy source live |
| "FinOps owns the spreadsheet; eng owns the constant" | Spreadsheet and `CACHE_TTL_SECONDS` diverge within one sale event |

## The Aha

**default_ttl_sec is operational config** — it changes during CDN anomalies, flash sales, and cost incidents. It belongs in a **live tree** the cache layer already reads with `getInt()`, not in a constant imported at JVM boot.

## What Kiponos.io is for cache freshness vs spend

[Kiponos.io](https://kiponos.io) is a real-time configuration hub with Java and Python SDKs. `Kiponos.createForCurrentTeam()` connects over WebSocket; the profile tree — for example `['catalog']['prod']['cache']` — hydrates into **in-process memory** at service startup.

When Jonah sets `paths/browse/default_ttl_sec` to `600`, a **delta** patches only that key. The next `kiponos.path("paths", "browse").getInt("default_ttl_sec")` on a cache write is a **local memory read** — no HTTP to a config API, no poll loop, no extra Redis round-trip for policy.

`afterValueChanged` logs TTL flips and can invalidate local Caffeine regions for affected path classes **without** restarting catalog pods.

No restart. No redeploy. No `@RefreshScope` bean recycle.

Honest boundary: Kiponos does **not** replace your CDN provider console for edge rules, Terraform for distribution config, or image optimization pipelines. It owns **application-layer TTL policy** Java services read on every cache operation.

## Architecture

![Architecture diagram](https://files.catbox.moe/mynwiy.png)

**CDN portal documents edge defaults; authoritative app TTL ladders live in Kiponos** where tuning them takes seconds.

## Config tree — paths, defaults, posture, purge, and audit

Five folders — `defaults`, `paths`, `posture`, `purge`, `audit`:

```yaml
defaults/
  default_ttl_sec: 300
  min_ttl_sec: 15
  max_ttl_sec: 3600
  enforce_path_overrides: true
paths/
  browse/
    default_ttl_sec: 600
    enabled: true
  category/
    default_ttl_sec: 900
    enabled: true
  pdp/
    default_ttl_sec: 120
    enabled: true
  search/
    default_ttl_sec: 30
    enabled: true
  inventory/
    default_ttl_sec: 15
    enabled: true
posture/
  cost_saver_mode: false
  cost_saver_multiplier: 2.0
  shed_stale_revalidate: false
purge/
  aggressive_purge_disabled: false
  purge_on_write_paths: ["pdp", "search"]
  batch_purge_interval_sec: 60
audit/
  last_ttl_change_by: ""
  last_ttl_change_at_ms: 0
  emit_ttl_metrics: true
```

One tree. One profile path: `['catalog']['prod']['cache']`. Staging CDN drills share **identical key layout** — only values differ.

## Java integration — path-class TTL resolver + cost-saver posture

```java
import io.kiponos.sdk.Kiponos;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

@Configuration
public class KiponosCacheConfig {
    @Bean
    public Kiponos kiponos() {
        Kiponos client = Kiponos.createForCurrentTeam();
        // Profile: ['catalog']['prod']['cache'] via -Dkiponos=... JVM arg
        client.afterValueChanged(change -> {
            log.info("Cache TTL delta: path={} value={}", change.path(), change.newValue());
            localCacheRegistry.invalidateMatching(change.path());
        });
        return client;
    }
}

@Service
public class CatalogCacheWriter {
    private final Kiponos kiponos;
    private final RedisCache redis;
    private final CdnClient cdn;

    public int resolveTtlSec(String pathClass) {
        var defaults = kiponos.path("defaults");
        int base = defaults.getInt("default_ttl_sec", 300);

        var pathCfg = kiponos.path("paths", pathClass);
        if (defaults.getBool("enforce_path_overrides", true)
            && pathCfg.exists() && pathCfg.getBool("enabled", true)) {
            base = pathCfg.getInt("default_ttl_sec", base);
        }

        var posture = kiponos.path("posture");
        if (posture.getBool("cost_saver_mode", false)) {
            double mult = posture.getDouble("cost_saver_multiplier", 2.0);
            base = (int) Math.min(base * mult, defaults.getInt("max_ttl_sec", 3600));
        }

        return Math.max(base, defaults.getInt("min_ttl_sec", 15));
    }

    public void put(String key, byte[] payload, String pathClass) {
        int ttl = resolveTtlSec(pathClass);
        redis.setex(key, ttl, payload);

        var purge = kiponos.path("purge");
        if (!purge.getBool("aggressive_purge_disabled", false)) {
            var purgePaths = purge.getList("purge_on_write_paths");
            if (purgePaths.contains(pathClass)) {
                cdn.purge(key);
            }
        }

        if (kiponos.path("audit").getBool("emit_ttl_metrics", true)) {
            metrics.record("catalog_cache_ttl_sec", ttl, "path", pathClass);
        }
    }
}

@Service
public class CatalogReadService {
    private final Kiponos kiponos;
    private final CatalogCacheWriter cacheWriter;

    public ProductJson getBrowseProduct(String sku) {
        return cacheLayer.getOrLoad("browse:" + sku,
            () -> origin.fetchProduct(sku),
            cacheWriter.resolveTtlSec("browse"));
    }

    public SearchResults search(String query) {
        var posture = kiponos.path("posture");
        if (posture.getBool("shed_stale_revalidate", false)) {
            return cacheLayer.getStaleAllowed("search:" + query,
                () -> searchIndex.query(query),
                cacheWriter.resolveTtlSec("search"));
        }
        return cacheLayer.getOrLoad("search:" + query,
            () -> searchIndex.query(query),
            cacheWriter.resolveTtlSec("search"));
    }
}
```

Every `getInt()`, `getBool()`, and `getDouble()` on the cache path is **O(1) local cache** — microseconds, not cross-region config service RTT.

**Image bytes and origin routing** stay in your CDN and object store — Kiponos owns the **TTL ladders** that change when egress alarms fire.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| CDN bill spike — shorten TTL on non-critical paths only | Global constant change + deploy; search freshness regresses | Raise `paths/browse` and `paths/category` TTL live; search stays 30s |
| Flash sale purge storm | Purge-on-write multiplies egress | `purge/aggressive_purge_disabled: true` from dashboard |
| FinOps cost-saver window | Manual CDN portal edits per distribution | `posture/cost_saver_mode: true` doubles non-critical TTLs |
| Inventory accuracy incident | Emergency deploy to lower search TTL | `paths/search/default_ttl_sec: 15` in one edit |
| Post-sale restore | Second deploy to reset constants | Reset `paths` and `posture` subtree in dashboard |

## Performance — hot path economics on cache writes

- **TTL resolution per write** — three local reads (defaults, path, posture); no HTTP on cache path
- **Path-class nesting** — browse, category, pdp, search each get a folder; no `BROWSE_TTL` env var matrix
- **Delta updates** — changing browse TTL sends one patch; existing keys expire naturally
- **afterValueChanged invalidation** — local Caffeine regions drop stale entries on policy flip
- **One WebSocket per JVM** — background sync; cache writes never block on config API RTT
- **Complements CDN edge rules** — portal owns edge; app owns **write-time TTL authority**

## Compare to alternatives

| Approach | Mid-spike TTL tune | Per-path-class TTL | Purge posture flip |
|----------|-------------------|--------------------|--------------------|
| YAML + redeploy | Poor — rolling restart | Awkward — nested YAML | Code change |
| CDN portal only | Good for edge — not app writes | Medium — disconnected from Redis TTL | Manual per distribution |
| Redis CONFIG SET | Runtime but global | Poor — not path-aware | N/A |
| Feature-flag SaaS | Booleans only | Awkward for integer seconds | Not ops-owned |
| Spreadsheet + human | N/A | Humans edit; app unchanged | Bridge chaos |
| **Kiponos live hub** | **Seconds — dashboard delta** | **Per-path subtree** | **One purge boolean** |

## When not to use Kiponos

| Case | Use instead |
|------|-------------|
| CDN distribution geography and TLS certs | CDN provider console / Terraform |
| Image transformation and WebP negotiation | Image pipeline / CDN native features |
| Cache key hashing and serialization format | Application code — Git-reviewed |
| Immutable CDN invoice reconciliation | FinOps warehouse — not live config |
| One-time bootstrap TTL from architecture review | `application.yml` at deploy time |

## Getting started (15 minutes)

1. Sign up at [kiponos.io](https://kiponos.io) (TeamPro).
2. Create profile path `['catalog']['prod']['cache']`.
3. Add `defaults/default_ttl_sec`, `paths/browse/default_ttl_sec`, and wire `resolveTtlSec()` in your cache write path.
4. `./gradlew bootRun` — confirm log shows WebSocket handshake.
5. Raise `paths/browse/default_ttl_sec` in dashboard; confirm new cache writes use longer TTL **without** JVM restart.
6. Drill: enable `posture/cost_saver_mode` in staging; watch egress-sensitive paths extend TTL.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [Black Friday runbook live](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-black-friday-runbook-live.md)
- [GPU dollars per request](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-finops-gpu-dollars-per-request.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*default_ttl_sec belongs in the live ops tree — not in constants that mock your FinOps team during the next CDN bill spike.*