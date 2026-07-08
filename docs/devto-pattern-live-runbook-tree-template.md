---
title: "Live Runbook Tree Template — Peak Posture in One Nested Profile (Java SDK)"
published: false
tags: java, sre, architecture, devops
description: Template for Black Friday-style runbooks as live config trees — reusable pattern from arch runbook article.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-live-runbook-tree-template.md
main_image: https://files.catbox.moe/uiggaj.jpg
---

SRE guild session. Someone shares a **forty-page Confluence runbook** for peak season: seventeen steps, four Helm value files, three feature-flag toggles, and a footnote that says *"rolling restart required."* The guild lead asks for something copy-pasteable:

> "Give us a **tree template** — folders, keys, Java wiring — so every team's runbook is a **dashboard flip**, not a deploy sequence."

This pattern article ships a **reusable live runbook tree** derived from [Black Friday runbooks as live config](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-black-friday-runbook-live.md). [Kiponos.io](https://kiponos.io) holds peak posture — rate limits, cache TTLs, queue depths, degradation flags — in profile `['runbook']['peak']['posture']`. Clone the template for **peak event — flip runbook tree in one edit**.

## The problem — runbooks as deploy archaeology

Peak runbooks describe **operational knobs** trapped in non-executable artifacts:

```java
private static final boolean PEAK_MODE = false;
private static final int RATE_LIMIT_RPM = 4000;
```

```yaml
# values-peak.yaml — committed days early
peak:
  rate_limit_rpm: 9000
  cache_ttl_sec: 300
```

Confluence step 14: *"Merge values-peak and rolling restart cart-api."* Traffic does not wait for step 14.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Runbook rehearsal equals readiness" | Rehearsal without **live tree** is theater |
| "Peak YAML merged Wednesday is fine" | Traffic shape wrong by peak hour |
| "Each service owns its peak constants" | Coordinated posture **diverges** |
| "Auto-scale replaces runbook limits" | New pods read **normal-week** constants |
| "One template cannot fit all teams" | **Folder contract** scales; values differ |

## The Aha

**A runbook is a narrative; the tree is the executable.** Standardize folder layout (`peak`, `cache`, `queues`, `degrade`, `audit`) so every service team maps runbook steps to **dashboard keys** — one `peak_mode_enabled: true` flip coordinates dozens of knobs.

## What Kiponos.io is for runbook trees

[Kiponos.io](https://kiponos.io) hydrates profile `['runbook']['peak']['posture']` over WebSocket. SRE flips `peak.mode_enabled`; deltas patch rate limits, TTLs, and degradation flags. Services read locally on hot paths — no restart.

## Architecture

![Architecture diagram](https://files.catbox.moe/kbwgf8.png)

## Template config tree — copy this skeleton

Rename values per service; **keep folder names**.

```yaml
peak/
  mode_enabled: false
  rate_limit_per_ip: 60
  peak_rate_limit_per_ip: 180
  global_rpm: 4000
  peak_global_rpm: 9000
  return_retry_after_sec: 5
  auto_enable_on_traffic_multiplier: 3.5
cache/
  default_ttl_sec: 60
  peak_default_ttl_sec: 300
  product_ttl_sec: 120
  peak_product_ttl_sec: 600
  inventory_ttl_sec: 15
queues/
  checkout_max_depth: 2000
  peak_checkout_max_depth: 8000
  reject_above_depth: true
  async_worker_concurrency: 16
  peak_async_worker_concurrency: 32
degrade/
  recommendations_enabled: true
  shed_noncritical_paths: false
  noncritical_paths: ["/v1/recommendations", "/v1/reviews"]
  wishlist_writes_enabled: true
  partner_webhooks_enabled: true
audit/
  last_peak_flip_by: ""
  last_peak_flip_at_ms: 0
  traffic_multiplier_last: 1.0
  runbook_version: "template-v1"
```

Profile path: `['runbook']['peak']['posture']`.

**Adoption rule:** every runbook step must cite a **tree path** — e.g. step 7 → `cache.peak_product_ttl_sec`.

## Template Java integration — peak-aware gate

```java
import io.kiponos.sdk.Kiponos;
import org.springframework.stereotype.Service;

@Service
public class RunbookPeakGate {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();

    public RunbookPeakGate() {
        kiponos.afterValueChanged(change ->
            log.info("Runbook tree delta: {} → {}", change.path(), change.newValue())
        );
    }

    public int effectiveRateLimitPerIp() {
        var peak = kiponos.path("peak");
        return peak.getBool("mode_enabled")
            ? peak.getInt("peak_rate_limit_per_ip")
            : peak.getInt("rate_limit_per_ip");
    }

    public int effectiveGlobalRpm() {
        var peak = kiponos.path("peak");
        return peak.getBool("mode_enabled")
            ? peak.getInt("peak_global_rpm")
            : peak.getInt("global_rpm");
    }

    public int effectiveProductTtl() {
        var cache = kiponos.path("cache");
        return kiponos.path("peak").getBool("mode_enabled")
            ? cache.getInt("peak_product_ttl_sec")
            : cache.getInt("product_ttl_sec");
    }

    public boolean shouldShedPath(String httpPath) {
        var degrade = kiponos.path("degrade");
        if (!degrade.getBool("shed_noncritical_paths")) return false;
        if (!kiponos.path("peak").getBool("mode_enabled")) return false;
        return degrade.get("noncritical_paths").asStringList().contains(httpPath);
    }

    public void recordTrafficMultiplier(double mult) {
        kiponos.path("audit").set("traffic_multiplier_last", mult);
        var peak = kiponos.path("peak");
        if (!peak.getBool("mode_enabled")
            && mult >= peak.getFloat("auto_enable_on_traffic_multiplier")) {
            peak.set("mode_enabled", true);
            kiponos.path("audit").set("last_peak_flip_by", "traffic_auto");
        }
    }
}
```

Wire `RunbookPeakGate` into rate limiters, cache resolvers, and non-critical route filters.

## Runbook ↔ tree mapping table (template)

| Runbook step (narrative) | Tree key | Normal | Peak |
|--------------------------|----------|--------|------|
| Enable peak mode | `peak.mode_enabled` | false | true |
| Raise per-IP limit | `peak.peak_rate_limit_per_ip` | 60 | 180 |
| Raise global RPM | `peak.peak_global_rpm` | 4000 | 9000 |
| Extend product cache | `cache.peak_product_ttl_sec` | 120 | 600 |
| Deepen checkout queue | `queues.peak_checkout_max_depth` | 2000 | 8000 |
| Shed recommendations | `degrade.shed_noncritical_paths` | false | true |
| Record who flipped | `audit.last_peak_flip_by` | — | on-call ID |

## Real scenarios

| Event | Confluence-only runbook | Live tree template |
|-------|-------------------------|-------------------|
| Peak event — flip runbook tree in one edit | Multi-step deploy | `peak.mode_enabled: true` |
| Traffic exceeds rehearsal | Stale Wednesday YAML | `traffic_auto` via `recordTrafficMultiplier` |
| Wind-down | Second deploy wave | `peak.mode_enabled: false` |
| New team onboard | Copy 40-page doc | Clone tree skeleton + map 6 keys |
| Audit post-peak | Git archaeology | `audit.*` keys in hub log |

## Performance

- **Peak mode check: 4–8 local reads** — noise vs I/O
- **Single WebSocket delta** enables coordinated posture — not N Helm releases
- **No bean recycle** for TTL and limit changes
- **Auto-enable** avoids human lag when multiplier threshold breached
- **Template folders** prevent per-team key invention on hot path

## Compare to alternatives

| Approach | One-edit peak flip | Coordinated knobs | Executable runbook |
|----------|-------------------|-------------------|-------------------|
| Confluence + Helm | No | Partial | No |
| Per-service constants | No | No | No |
| Feature flags (booleans only) | Partial | No TTL/queue ints | Partial |
| **Kiponos template tree** | **Yes** | **Yes** | **Yes** |

## When not to use this template

| Boundary | Better home |
|----------|-------------|
| Cluster node pool capacity | Cloud autoscaler |
| CDN edge purge | Vendor API |
| Database schema migrations | GitOps |
| Payment certificates | Vault |
| Marketing copy | CMS |

## Getting started (15 minutes) — adopt template

1. Copy YAML skeleton into profile `['runbook']['peak']['posture']` (or `['your-service']['prod']['live']`).
2. Map your Confluence runbook steps to tree keys using mapping table.
3. Paste `RunbookPeakGate` pattern; wire three call sites (rate, cache, degrade).
4. Staging game day: flip `peak.mode_enabled` — verify **no restart**.
5. Set `audit.runbook_version` to `your-team-v1` for change tracking.
6. Retire Helm `values-peak.yaml` keys that duplicate tree.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [Black Friday runbook article](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-black-friday-runbook-live.md)
- [Peak event posture flip pattern](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-peak-event-posture-flip.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — clone the tree template once; your runbook stops being a deploy checklist at midnight.*