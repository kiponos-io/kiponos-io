---
title: "Cache TTL Ladders — Live Edge Freshness Without Purge Tickets (Java SDK)"
published: false
tags: java, cdn, edge, architecture
description: CDN TTL tiers in provider UI are not API-hot-path knobs. Kiponos holds TTL ladders edge workers read locally.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-edgeops-cache-ttl-ladders.md
main_image: https://files.catbox.moe/i8rg0m.jpg
---

Tuesday 11:47 UTC. **catalog-origin** p95 latency crosses **2.8s** — three times the SLO — while edge POPs still serve product JSON with `Cache-Control: max-age=120`. The CDN console shows a global default TTL of 120 seconds. Extending TTL on `/static/*` and `/product/*` without touching `/inventory/*` means opening a **purge ticket**, waiting for vendor approval, and hoping the rule propagates before Postgres connection pools exhaust.

The edge SRE on the bridge:

> "We need **tiered TTL ladders** we can stretch on static paths **right now** — not a global CDN UI change that takes forty minutes while origin drowns."

Most Java edge gateways and BFF layers encode cache policy as **boot-time constants** or nginx snippets that require reload. [Kiponos.io](https://kiponos.io) holds **TTL ladders** — per-path tiers, peak overrides, and origin-protection modes — in a live tree edge workers read on every outbound cache header decision with **local `getInt()`** and no worker restart.

## The problem — `default_ttl_sec` frozen at the edge hot path

A typical Spring Boot edge BFF sets cache headers from constants:

```java
@Service
public class EdgeCacheHeaderService {
    private static final int DEFAULT_TTL = 120;
    private static final int STATIC_TTL = 3600;
    private static final int INVENTORY_TTL = 15;

    public CacheDirective forPath(String path) {
        if (path.startsWith("/static/")) {
            return CacheDirective.maxAge(STATIC_TTL);
        }
        if (path.startsWith("/inventory/")) {
            return CacheDirective.maxAge(INVENTORY_TTL);
        }
        return CacheDirective.maxAge(DEFAULT_TTL);
    }
}
```

CDN vendor defaults mirror the same rigidity:

```yaml
# cloudfront-distribution.tf — infra desired state, not incident knob
default_cache_behavior:
  min_ttl: 0
  default_ttl: 120
  max_ttl: 86400
```

During **origin struggling — extend TTL on static assets only**, you need to:

1. Raise **`ladders.static.ttl_sec`** from 3600 → 7200 without touching inventory freshness
2. Enable **`origin_protect.mode_enabled`** to apply emergency ladder globally on browse paths
3. Lower **`ladders.api.default_ttl_sec`** only if you are deliberately sacrificing freshness for a failing shard

Terraform and nginx reloads are measured in **minutes**. Origin connection exhaustion is measured in **seconds**.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "CDN console TTL is enough" | Console changes are **global** and **slow** — not per-route hot-path reads |
| "We purge and extend in the vendor UI" | Purge tickets queue behind other customers during regional incidents |
| "Edge workers should read origin Cache-Control only" | Origin headers reflect **deploy-time** policy, not bridge decisions |
| "Peak TTL belongs in values-peak.yaml" | Wednesday's peak YAML is wrong when origin latency spikes Thursday |
| "Redis cache in BFF is separate from CDN TTL" | Without a shared ladder tree, BFF and CDN tiers **diverge** under stress |

## The Aha

**Cache TTL is an operational shield for origin** — it stretches when databases strain and tightens when freshness matters. TTL tiers belong in a **live ladder tree** the edge worker reads with `getInt()` on every response, not in `static final` constants imported at JVM boot.

## What Kiponos.io is for edge TTL ladders

[Kiponos.io](https://kiponos.io) is a real-time configuration hub. Each edge BFF or worker connects once via WebSocket; profile `['edge']['prod']['cache']` hydrates an in-memory tree inside the Java SDK.

When SRE enables `origin_protect.mode_enabled` and sets `ladders.static.emergency_ttl_sec` to 7200, a **delta** patches only those keys. The next `kiponos.path("ladders", "static").getInt("emergency_ttl_sec")` on an outgoing `/static/logo.svg` response is a **local memory read** — no HTTP to a config API, no CDN API poll, no pod restart.

`afterValueChanged` listeners let you increment `ttl_ladder_change_total`, log audit trails, and warm longer-TTL regions in local edge caches without recycling workers.

## Architecture

![Architecture diagram](https://files.catbox.moe/696qnh.png)

## Config tree — TTL ladders, origin protect, peak tiers

Five folders — `ladders`, `origin_protect`, `peak`, `paths`, `audit`:

```yaml
ladders/
  default_ttl_sec: 120
  api/
    default_ttl_sec: 90
    product_ttl_sec: 180
    category_ttl_sec: 300
  static/
    ttl_sec: 3600
    emergency_ttl_sec: 7200
    font_ttl_sec: 86400
  inventory/
    ttl_sec: 15
    stale_while_revalidate_sec: 5
origin_protect/
  mode_enabled: false
  apply_to_prefixes: ["/static/", "/product/", "/category/"]
  multiplier: 2.0
  max_ceiling_sec: 7200
peak/
  mode_enabled: false
  browse_ttl_multiplier: 1.5
  static_ttl_override_sec: 5400
paths/
  bypass_ttl_prefixes: ["/inventory/", "/cart/checkout"]
  force_no_cache: ["/account/session"]
audit/
  last_ladder_flip_by: ""
  last_origin_protect_at_ms: 0
```

Profile path: `['edge']['prod']['cache']`. One tree shared across every edge POP's JVM workers.

## Java integration — ladder-aware cache headers on the hot path

```java
import io.kiponos.sdk.Kiponos;
import org.springframework.stereotype.Service;

@Service
public class LadderCacheHeaderService {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();

    public LadderCacheHeaderService() {
        kiponos.afterValueChanged(change ->
            log.info("TTL ladder delta: {} → {}", change.path(), change.newValue())
        );
    }

    public CacheDirective resolve(String requestPath) {
        if (isBypass(requestPath)) {
            return CacheDirective.noStore();
        }
        if (requestPath.startsWith("/static/")) {
            return CacheDirective.maxAge(resolveStaticTtl());
        }
        if (requestPath.startsWith("/inventory/")) {
            var inv = kiponos.path("ladders", "inventory");
            return CacheDirective.maxAge(inv.getInt("ttl_sec"));
        }
        if (requestPath.startsWith("/product/")) {
            return CacheDirective.maxAge(resolveApiTtl(requestPath, "product_ttl_sec", 180));
        }
        return CacheDirective.maxAge(
            kiponos.path("ladders").getInt("default_ttl_sec", 120)
        );
    }

    private int resolveStaticTtl() {
        var protect = kiponos.path("origin_protect");
        var staticLadder = kiponos.path("ladders", "static");
        if (protect.getBool("mode_enabled")) {
            return staticLadder.getInt("emergency_ttl_sec");
        }
        if (kiponos.path("peak").getBool("mode_enabled")) {
            int override = kiponos.path("peak").getInt("static_ttl_override_sec", 0);
            if (override > 0) return override;
        }
        return staticLadder.getInt("ttl_sec");
    }

    private int resolveApiTtl(String requestPath, String key, int fallback) {
        int base = kiponos.path("ladders", "api").getInt(key, fallback);
        var protect = kiponos.path("origin_protect");
        if (protect.getBool("mode_enabled") && matchesProtectPrefix(requestPath)) {
            double mult = protect.getFloat("multiplier", 2.0);
            int ceiling = protect.getInt("max_ceiling_sec", 7200);
            return Math.min((int) (base * mult), ceiling);
        }
        return base;
    }

    private boolean isBypass(String path) {
        return kiponos.path("paths", "bypass_ttl_prefixes")
            .asStringList().stream().anyMatch(path::startsWith);
    }

    private boolean matchesProtectPrefix(String path) {
        return kiponos.path("origin_protect", "apply_to_prefixes")
            .asStringList().stream().anyMatch(path::startsWith);
    }
}
```

Every `getInt()` and `getBool()` on the response path is **O(1) in-process** — microseconds beside TLS and serialization.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| Origin struggling — extend TTL on static assets only | CDN ticket + Terraform; global side effects | `origin_protect.mode_enabled: true`; static ladder reads emergency tier |
| Regional browse spike | Short product TTL hammers catalog DB | `peak.browse_ttl_multiplier: 1.5` live |
| Inventory flash sale | Static TTL unchanged; wrong lever | Bypass list keeps `/inventory/*` at 15s |
| Post-incident wind-down | Second deploy to restore ladders | `origin_protect.mode_enabled: false` — one edit |
| Multi-POP consistency | Per-POP nginx drift | Shared profile tree; identical ladder semantics |

## Performance on the edge hot path

- **One WebSocket per edge JVM** — not one config fetch per HTTP response
- **Ladder resolution is 3–6 local reads** — nanoseconds vs origin RTT at 2.8s p95
- **Delta patches single keys** — enabling origin protect does not reload the full tree
- **No `@RefreshScope` recycle** — workers keep connection pools warm during origin stress
- **CDN miss ratio drops immediately** — longer `max-age` on next response without vendor API latency

## Compare to alternatives

| Approach | Per-route TTL during incident | Hot-path read latency | Tiered ladders |
|----------|------------------------------|----------------------|----------------|
| CDN vendor console | Slow; often global | N/A (not in app) | Awkward rules UI |
| Terraform / CloudFront API | Minutes; PR culture | N/A | Infra-state coupling |
| nginx `proxy_cache_valid` reload | Reload per POP | Zero if static at boot | Per-file config drift |
| Redis config hash + poll | Yes with poll interval | Poll staleness on path | DIY schema |
| Spring Cloud Config refresh | Network + bean recycle | Poor | Flat keys |
| **Kiponos live hub** | **Seconds — dashboard** | **Local getInt()** | **Nested ladders/** |

## When not to use Kiponos

| Case | Use instead |
|------|-------------|
| CDN distribution ARN, certificate, WAF rule IDs | Terraform / vendor IaC |
| Full-cache purge of compromised assets | CDN purge API — correctness over speed |
| Legal takedown of specific URLs | Vendor abuse workflow |
| Bootstrap: which origins exist | GitOps Helm values |
| Immutable audit record of what was served | Access logs + SIEM — not live TTL |

## Getting started (15 minutes)

1. [Create TeamPro at kiponos.io](https://kiponos.io) — profile `['edge']['prod']['cache']` with `ladders`, `origin_protect`, `peak`, and `paths` folders above.
2. Add `io.kiponos:sdk-boot-3` to your edge BFF Spring Boot 3 service.
3. Set `KIPONOS_ID`, `KIPONOS_ACCESS`, and `-Dkiponos="['edge']['prod']['cache']"`.
4. Replace `DEFAULT_TTL` and `STATIC_TTL` constants with `LadderCacheHeaderService` ladder reads.
5. Load test: simulate origin latency spike; flip `origin_protect.mode_enabled: true` — confirm `/static/*` `max-age` increases **without pod restart**.
6. Document ladder key names in your origin-protection runbook beside CDN escalation steps.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [Cache freshness vs spend](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-finops-cache-freshness-vs-spend.md)
- [Black Friday runbook live tree](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-black-friday-runbook-live.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — CDN consoles protect the edge network; TTL ladders protect your origin when latency spikes.*