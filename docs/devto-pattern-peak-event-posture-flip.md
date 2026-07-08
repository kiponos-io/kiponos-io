---
title: "Peak Event Posture Flip — One Tree, Many Knobs (Java SDK)"
published: false
tags: java, sre, architecture, devops
description: Pattern for flipping rate limits, TTLs, pool sizes together during peaks.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-peak-event-posture-flip.md
main_image: https://files.catbox.moe/id94bo.jpg
---

Cyber Monday 00:04 UTC. Traffic **5.1×** baseline. Three engineers execute runbook steps in parallel: one raises rate limits in Redis, one edits cache TTL in Helm, one toggles a feature flag for recommendations. Limits rise before TTLs extend; catalog Postgres saturates; checkout still capped at normal-week RPM.

The SRE manager:

> "Peak posture is **one coordinated flip** — not three tools drifting apart. I want **`posture.mode: peak`** to raise RPM, TTL, and pool size **together**."

This pattern defines **peak event posture flip** — a single boolean (or enum) drives many knobs in one Kiponos profile. [Kiponos.io](https://kiponos.io) profile `['peak']['prod']['posture']` holds coordinated limits for **Cyber Monday — single posture flip**.

## The problem — uncorrelated peak knobs

```java
private static final int RATE_LIMIT_RPM = 4000;
private static final int CACHE_TTL_SEC = 60;
private static final int WORKER_POOL = 16;
```

Each knob in a different store:

| Knob | Store | Flip latency |
|------|-------|--------------|
| RPM 4000 → 9000 | Redis | Manual |
| Cache TTL 60 → 300 | Helm | PR |
| Pool 16 → 32 | ConfigMap | Rollout |

**Partial peak posture** is worse than none — higher RPM with short TTL **amplifies** origin load.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Flip limits first, TTL later" | Origin dies in the gap |
| "Peak flags are booleans enough" | TTLs and RPM are **ints** — flags awkward |
| "Each team owns its peak keys" | Coordination **fails** at 00:04 UTC |
| "Auto-scale is peak posture" | New pods inherit **normal** constants |
| "Runbook order is sufficient" | Humans parallelize under stress |

## The Aha

**Peak posture is a mode, not a scatter plot of keys.** One `posture.mode_enabled` (or `posture.mode: peak`) selects **entire subtrees** — rate limits, TTLs, pools, degradation — read atomically from local tree on each request.

## What Kiponos.io is for posture flip

WebSocket hub; profile `['peak']['prod']['posture']`. Flip `posture.mode_enabled: true`; services resolve **peak vs normal** pairs via local `getBool()` + `getInt()` — no restart.

## Architecture

![Architecture diagram](https://files.catbox.moe/g8k6uy.png)

## Config tree — normal/peak pairs under one posture root

```yaml
posture/
  mode_enabled: false
  mode_name: normal
  auto_enable_multiplier: 4.0
rate_limits/
  normal_rpm: 4000
  peak_rpm: 9000
  normal_per_ip: 60
  peak_per_ip: 180
cache/
  normal_product_ttl_sec: 60
  peak_product_ttl_sec: 300
  normal_category_ttl_sec: 120
  peak_category_ttl_sec: 600
workers/
  normal_pool_size: 16
  peak_pool_size: 32
  normal_queue_depth: 2000
  peak_queue_depth: 8000
degrade/
  peak_shed_recommendations: true
  peak_shed_reviews: true
audit/
  last_posture_flip: normal
  last_flip_at_ms: 0
  last_flip_by: ""
```

Profile path: `['peak']['prod']['posture']`.

## Integration — posture resolver pattern (Java)

```java
import io.kiponos.sdk.Kiponos;
import org.springframework.stereotype.Component;

@Component
public class PeakPostureResolver {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();

    public PeakPostureResolver() {
        kiponos.afterValueChanged(change -> {
            if (change.path().contains("posture/mode_enabled")) {
                log.warn("Peak posture flip: mode_enabled={}", change.newValue());
            }
        });
    }

    public boolean isPeak() {
        return kiponos.path("posture").getBool("mode_enabled");
    }

    public int rateLimitRpm() {
        var rl = kiponos.path("rate_limits");
        return isPeak() ? rl.getInt("peak_rpm") : rl.getInt("normal_rpm");
    }

    public int rateLimitPerIp() {
        var rl = kiponos.path("rate_limits");
        return isPeak() ? rl.getInt("peak_per_ip") : rl.getInt("normal_per_ip");
    }

    public int productCacheTtlSec() {
        var cache = kiponos.path("cache");
        return isPeak() ? cache.getInt("peak_product_ttl_sec") : cache.getInt("normal_product_ttl_sec");
    }

    public int workerPoolSize() {
        var workers = kiponos.path("workers");
        return isPeak() ? workers.getInt("peak_pool_size") : workers.getInt("normal_pool_size");
    }

    public boolean shouldShedRecommendations() {
        return isPeak() && kiponos.path("degrade").getBool("peak_shed_recommendations");
    }

    public void onTrafficMultiplier(double mult) {
        var posture = kiponos.path("posture");
        if (!isPeak() && mult >= posture.getFloat("auto_enable_multiplier")) {
            posture.set("mode_enabled", true);
            posture.set("mode_name", "peak");
            kiponos.path("audit").set("last_flip_by", "traffic_auto");
        }
    }
}
```

```java
@Service
public class CartService {
    private final PeakPostureResolver posture;

    public CartResponse addToCart(String ip, Request req) {
        if (!rateLimiter.tryAcquire(ip, posture.rateLimitPerIp())) {
            throw new TooManyRequestsException();
        }
        int ttl = posture.productCacheTtlSec();
        return cartLogic(req, catalogCache.get(req.sku(), ttl));
    }
}
```

### Python checkout worker — same posture profile

```python
import os
from kiponos import Kiponos

os.environ["KIPONOS_PROFILE"] = "['peak']['prod']['posture']"
kiponos = Kiponos.create_for_current_team()

def is_peak() -> bool:
    return kiponos.path("posture").get_bool("mode_enabled")

def worker_pool_size() -> int:
    w = kiponos.path("workers")
    return w.get_int("peak_pool_size") if is_peak() else w.get_int("normal_pool_size")
```

## Posture flip runbook — single edit

| Action | Tree change | Effect |
|--------|-------------|--------|
| Enter peak | `posture.mode_enabled: true` | All resolvers select peak_* keys |
| Exit peak | `posture.mode_enabled: false` | Restore normal_* |
| Auto peak | Traffic ≥ `auto_enable_multiplier` | `traffic_auto` flip |
| Audit | `audit.last_flip_by` | Hub actor log |

## Real scenarios

| Event | Scattered peak knobs | Single posture flip |
|-------|----------------------|---------------------|
| Cyber Monday — single posture flip | Three tools; partial state | One boolean |
| Origin protection | TTL raised; RPM still low | Coordinated peak pair |
| Wind-down | Missed Redis key | `mode_enabled: false` |
| Load test rehearsal | Staging diverges | Same tree layout |
| Auto-enable | Human asleep at 00:04 | `onTrafficMultiplier` |

## Performance

- **`isPeak()` one read** — amortized across resolver methods per request
- **Single delta** on flip — not N ConfigMap rollouts
- **Normal/peak pairs in memory** — no runtime math beyond branch
- **Python + Java share profile** — checkout worker and cart-api aligned
- **No `@RefreshScope`** — pool size via binder optional; reads immediate

## Compare to alternatives

| Approach | Coordinated flip | Int knobs + bool mode | Sub-second |
|----------|------------------|----------------------|------------|
| Redis + Helm + flags | No | Partial | Minutes |
| Confluence runbook | No | Narrative only | Hours |
| **Kiponos posture tree** | **Yes** | **Yes** | **Seconds** |

## When not to use this pattern

| Boundary | Better home |
|----------|-------------|
| Cluster autoscaling min/max | Kubernetes |
| CDN edge rules | Vendor |
| Permanent capacity planning | Architecture RFC |
| Product A/B during peak | Feature flags |

## Getting started (15 minutes)

1. Create `['peak']['prod']['posture']` with normal/peak pairs above.
2. Implement `PeakPostureResolver`; inject into cart, catalog, checkout.
3. Replace scattered peak constants with resolver methods.
4. Staging: single flip `posture.mode_enabled: true` — verify RPM, TTL, pool together.
5. Wire `onTrafficMultiplier` to traffic dashboard metric.
6. Runbook: one step — *"Enable peak posture in Kiponos."*

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [Live runbook tree template](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-live-runbook-tree-template.md)
- [Black Friday runbook](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-black-friday-runbook-live.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — peak posture is one flip, not three engineers and four tools at midnight.*