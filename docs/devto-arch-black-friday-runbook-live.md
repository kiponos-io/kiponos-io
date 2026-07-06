---
title: "Black Friday Runbooks as Live Config — Scale Limits, Cache TTLs, and Queue Depths in One Tree (Java SDK)"
published: false
tags: java, retail, architecture, sre
description: Peak-season knobs trapped in Confluence mean midnight deploys on Thanksgiving. Kiponos holds rate limits, cache TTLs, and queue thresholds in one live tree — SRE flips peak posture while JVMs keep running.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-black-friday-runbook-live.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-arch-black-friday-runbook-live.jpg
---

Friday 00:01 UTC — **Black Friday**. **cart-api** traffic is **4.2×** normal and climbing. The seasonal runbook page 14 says: *"set `RATE_LIMIT_PER_IP=120` in `values-peak.yaml`, extend `PRODUCT_CACHE_TTL_SEC` to 300, raise `CHECKOUT_QUEUE_MAX` to 8000 — commit and rolling restart all cart pods by 23:00 Thanksgiving."*

Someone committed at 21:40. Rolling restart finished at 23:58. For two hours peak traffic hit **pre-peak limits** — aggressive rate limiting dropped legitimate mobile clients, short cache TTLs hammered catalog Postgres, and checkout queue depth capped at 2000 spawned **HTTP 503** storms on the payment handoff.

The retail SRE lead asks:

> "Why is our **most rehearsed runbook** still a **deploy sequence** when peak arrives **once per year** and every knob is operational?"

Most Java retail services encode peak posture as **three different artifacts**: a Confluence seasonal checklist, environment-specific Helm values committed days early, and `static final` cache TTLs that only change after a restart. [Kiponos.io](https://kiponos.io) collapses that into **one operational tree** — rate limits, cache TTLs, queue depths, and feature degradations — readable on every cart request with **local `get*()` calls** and flippable from the dashboard while processes run.

## The problem: peak-season knobs baked into immutable config

A typical cart service gates peak behavior like this:

```java
@Service
public class CartRateLimiter {
    private static final int RATE_LIMIT_PER_IP = 60;
    private static final int PRODUCT_CACHE_TTL_SEC = 60;

    public CartResponse addToCart(String ip, AddToCartRequest req) {
        if (!rateLimiter.tryAcquire(ip, RATE_LIMIT_PER_IP)) {
            throw new TooManyRequestsException();
        }
        Product product = catalogCache.get(req.getSku(), PRODUCT_CACHE_TTL_SEC);
        return cartService.add(req, product);
    }
}
```

Peak policy usually lives elsewhere — scattered and static:

```yaml
# values-peak.yaml — committed days early; wrong by midnight
cart:
  rate-limit-per-ip: 120
  product-cache-ttl-sec: 300
  checkout-queue-max: 8000
  degrade-recommendations: true
```

Or worse — peak flags never merged because the PR conflicted with a hotfix:

```java
private static final int RATE_LIMIT_PER_IP = 60;  // normal-week value on Black Friday
```

The cart path executes **tens of thousands of times per second** on peak day. You need to:

1. Raise **`peak.rate_limit_per_ip`** when mobile client mix shifts without dropping legit traffic
2. Extend **`peak.product_cache_ttl_sec`** to protect catalog Postgres during browse spikes
3. Flip **`peak.degrade_recommendations`** and **`peak.shed_noncritical_paths`** to preserve checkout

Doing that through Helm at midnight while dashboards burn red is not peak readiness — it is **seasonal theater with revenue on the line**.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "We deploy peak config Wednesday — we're fine" | Traffic shape shifts; Wednesday values wrong by Friday 00:30 |
| "Auto-scaling handles peak" | Scaling adds pods that still read **normal-week limits** |
| "Cache TTL is an engineering constant" | TTL is a **database protection knob** — different every hour on BFCM |
| "Runbook in Confluence is our source of truth" | Confluence does not gate `tryAcquire()` at 4.2× traffic |
| "We'll manually flip feature flags" | Flag console and cart service disagree on degradation scope |

## The architecture insight

**Black Friday posture is operational config, not seasonal deploy archaeology.** The same knobs your peak runbook tells SRE to edit — rate limits, cache TTLs, queue depths, degradation flags — belong in **one live tree** the JVM already reads on every cart request. Kiponos makes "peak mode ON" a **dashboard edit**, not a Thanksgiving deploy window.

## What Kiponos.io is for Black Friday runbooks

[Kiponos.io](https://kiponos.io) is a real-time configuration hub. Each Spring Boot cart service connects **once** at startup over WebSocket; the profile tree — for example `['retail']['cart']['prod']['live']` — loads into an **in-memory cache** inside the Java SDK.

When SRE sets `peak.mode_enabled` to `true` and `peak.rate_limit_per_ip` to `180`, a **delta** patches only those keys. The next `kiponos.path("peak").getInt("rate_limit_per_ip")` on an incoming add-to-cart is a **local memory read** — no HTTP to a config API, no JDBC poll, no Redis round-trip on the cart path.

`afterValueChanged` listeners let you log audit trails, increment `peak_mode_transition_total`, and warm extended-TTL cache regions **without** restarting the JVM.

No restart. No redeploy. No `@RefreshScope` bean recycle.

## Reference architecture

![Architecture diagram](https://files.catbox.moe/yhkd8n.png)

**Runbook documents the tree; the tree drives peak behavior.** Keep narrative and escalation charts in Confluence — but the **authoritative peak values** live in Kiponos where flipping them takes seconds.

## Config tree — peak, cache, queues, and degradation

Five folders — `peak`, `cache`, `queues`, `degrade`, `audit`:

```yaml
peak/
  mode_enabled: false
  rate_limit_per_ip: 60
  peak_rate_limit_per_ip: 180
  return_retry_after_sec: 5
  auto_enable_on_traffic_multiplier: 3.5
cache/
  product_ttl_sec: 60
  peak_product_ttl_sec: 300
  category_ttl_sec: 120
  peak_category_ttl_sec: 600
  inventory_ttl_sec: 15
queues/
  checkout_max_depth: 2000
  peak_checkout_max_depth: 8000
  reject_above_depth: true
degrade/
  recommendations_enabled: true
  shed_noncritical_paths: false
  noncritical_paths: ["/v1/recommendations", "/v1/reviews"]
  wishlist_writes_enabled: true
audit/
  last_peak_flip_by: ""
  last_peak_flip_at_ms: 0
  traffic_multiplier_last: 1.0
```

One tree. One profile path: `['retail']['cart']['prod']['live']`. Load-test rehearsals share **identical key layout** — only values differ.

## Java integration: peak-aware cart gate + live cache TTL

```java
import io.kiponos.sdk.Kiponos;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

@Service
public class PeakAwareCartService {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();
    private final RateLimiter rateLimiter;
    private final CatalogCache catalogCache;
    private final CartService cartService;

    public PeakAwareCartService(RateLimiter rateLimiter, CatalogCache catalogCache,
                                CartService cartService) {
        this.rateLimiter = rateLimiter;
        this.catalogCache = catalogCache;
        this.cartService = cartService;
        kiponos.afterValueChanged(change ->
            log.info("Peak config delta: path={} value={}", change.path(), change.newValue())
        );
    }

    public CartResponse addToCart(String ip, AddToCartRequest req) {
        var peak = kiponos.path("peak");
        int limit = peak.getBool("mode_enabled")
            ? peak.getInt("peak_rate_limit_per_ip")
            : peak.getInt("rate_limit_per_ip");

        if (!rateLimiter.tryAcquire(ip, limit)) {
            throw new TooManyRequestsException(peak.getInt("return_retry_after_sec"));
        }

        int ttl = resolveProductTtl();
        Product product = catalogCache.get(req.getSku(), ttl);
        return cartService.add(req, product);
    }

    private int resolveProductTtl() {
        var cache = kiponos.path("cache");
        return kiponos.path("peak").getBool("mode_enabled")
            ? cache.getInt("peak_product_ttl_sec")
            : cache.getInt("product_ttl_sec");
    }
}

@RestController
@RequestMapping("/v1")
public class CartController {
    private final PeakAwareCartService cartService;
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();

    @PostMapping("/cart/add")
    public CartResponse add(@RequestHeader("X-Forwarded-For") String ip,
                            @RequestBody AddToCartRequest req) {
        return cartService.addToCart(ip, req);
    }

    @GetMapping("/recommendations")
    public Recommendations recs(@RequestParam String sku) {
        var degrade = kiponos.path("degrade");
        if (!degrade.getBool("recommendations_enabled")
            || (kiponos.path("peak").getBool("mode_enabled")
                && degrade.getBool("shed_noncritical_paths"))) {
            return Recommendations.empty();
        }
        return recommendationEngine.forSku(sku);
    }
}

@Service
public class PeakAutoEnableService {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();

    public void onTrafficSample(double currentMultiplier) {
        kiponos.path("audit").set("traffic_multiplier_last", currentMultiplier);
        var peak = kiponos.path("peak");
        if (!peak.getBool("mode_enabled")
            && currentMultiplier >= peak.getFloat("auto_enable_on_traffic_multiplier")) {
            peak.set("mode_enabled", true);
            kiponos.path("audit").set("last_peak_flip_by", "traffic_auto");
        }
    }
}
```

Every `getInt()`, `getBool()`, and `getFloat()` on the cart path is **O(1) local cache** — microseconds, not cross-region config service RTT.

Wire `PeakAutoEnableService` to your traffic multiplier metric so peak mode engages when reality exceeds rehearsal — no one waits for the SRE who went to sleep at 23:55.

## Real-world scenarios

| Scenario | Without live peak tree | With Kiponos one-tree Black Friday runbook |
|----------|------------------------|--------------------------------------------|
| Midnight traffic exceeds rehearsal | Pods still at normal-week limits | `peak.mode_enabled: true` in dashboard |
| Catalog Postgres pressure | Short TTL hammering DB | `peak_product_ttl_sec: 300` live |
| Checkout queue 503 storm | Static 2000 depth cap | `peak_checkout_max_depth: 8000` |
| Shed recommendations load | Feature flag console lag | `degrade/shed_noncritical_paths: true` |
| Cyber Monday wind-down | Second deploy wave | `peak.mode_enabled: false` — single edit |

## Performance: why peak gates must not add network I/O

- **One WebSocket per JVM** — not one config fetch per cart request
- **Peak mode check is four local reads** — nanoseconds vs catalog and payment I/O
- **Delta patches** — enabling peak mode sends one patch, not full tree reload to every pod
- **Cache TTL via local int** — extended TTL applies on next `get()` without bean recycle
- **No GC pressure** from re-parsing peak YAML on every add-to-cart at 4.2× traffic

In load tests, Kiponos reads are noise on the cart path; catalog Postgres and payment handoff dominate latency.

## Compare to alternatives

| Approach | Midnight peak flip | Hot-path read latency | Single tree for limits + cache + queues |
|----------|-------------------|----------------------|----------------------------------------|
| Confluence + Helm PR | No — pipeline bound | Zero (static) but stale | No — scattered artifacts |
| Kubernetes ConfigMap | Minutes — relist + restart | Zero if baked at startup | Partial — no live flip |
| Redis config hash | Yes with poll | Poll interval adds tail latency | Possible — custom schema discipline |
| Feature-flag SaaS | Partial — booleans only | SDK network on evaluation | No — TTLs and queue depths awkward |
| Pre-committed values-peak.yaml | Wrong by peak hour | Zero (static) | Partial — deploy timing risk |
| **Kiponos SDK** | **Yes — dashboard delta** | **Zero (in-process cache)** | **Yes — one profile tree** |

## When not to use Kiponos for Black Friday peak config

| Boundary | Better home |
|----------|-------------|
| HPA min/max replicas, node pool autoscaling, cluster capacity | Kubernetes / cloud autoscaler |
| CDN cache purge, edge WAF rules, DDoS mitigation | CDN / security vendor consoles |
| Database connection pool **size** tied to instance memory | GitOps + DBA review — infrequent |
| Payment processor merchant IDs and acquirer certificates | Vault / PCI-scoped secrets |
| Marketing campaign content and SKU pricing catalog | PIM / merchandising systems |

## Getting started (15 minutes)

1. [Create TeamPro at kiponos.io](https://kiponos.io) — add profile `['retail']['cart']['prod']['live']` with `peak`, `cache`, `queues`, and `degrade` folders matching the tree above.
2. Add `io.kiponos:sdk-boot-3` to your Spring Boot cart service.
3. Set `KIPONOS_ID`, `KIPONOS_ACCESS`, and `-Dkiponos="['retail']['cart']['prod']['live']"`.
4. Replace `RATE_LIMIT_PER_IP` and `PRODUCT_CACHE_TTL_SEC` constants with `kiponos.path("peak", ...)` and `kiponos.path("cache", ...)`.
5. Register `PeakAwareCartService`, degradation checks on non-critical routes, and `PeakAutoEnableService` on traffic metrics.
6. Load test: in staging, flip `peak.mode_enabled: true` — confirm rate limits, cache TTLs, and recommendation shedding change **without pod restart**. Document key names in your Black Friday runbook.

## Further reading

- [Developer Quickstart](https://dev.to/kiponos/kiponosio-developer-quickstart-java-python-and-your-first-live-config-change-3kjo)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
- Related: [Disaster recovery live config](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-disaster-recovery-live-config.md)
- Related: [Dynamic retail pricing](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-retail-dynamic-pricing.md)

---

*Kiponos.io — your Black Friday runbook is a checklist; the tree is what protects Postgres at 4.2× traffic.*