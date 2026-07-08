---
title: "Hot-Path Config Checklist — Does This Knob Belong in Kiponos? (Architecture)"
published: false
tags: architecture, devops, java, python
description: Practical checklist for architects: latency, ownership, audit, nested structure.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-buy-guide-hot-path-config-checklist.md
main_image: https://files.catbox.moe/y1msbh.jpg
---

New microservice design review. The team proposes `permits_per_second` in `application.yml`, `block_score` in Redis with 10s poll, and `replicaCount` in the same Helm chart. The staff architect stops the review:

> "Before first deploy — run the **hot-path checklist**. If we wire this wrong, we spend two years migrating under fire."

This is the **operational admission checklist** for [Kiponos.io](https://kiponos.io) — does this knob belong in the live hub? Use it during **new microservice — run checklist before first deploy** and on every PR that adds configuration.

## The problem — knobs land in the wrong store by default

```java
// What teams ship on day one
private static final int PERMITS_PER_SECOND = 800;
```

```yaml
# application.yml — mixed fate
resilience4j:
  ratelimiter:
    instances:
      api:
        limitForPeriod: 800    # hot-path — wrong home
server:
  port: 8080                   # bootstrap — fine
```

```python
BLOCK_SCORE = 85  # module constant — incident pain later
```

Without a checklist, **every constant becomes technical debt** the on-call pays during the first BIN attack.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "We will externalize before prod" | Prod arrives; constants stay |
| "Redis is fine for hot-path floats" | Poll interval = **staleness + tail latency** |
| "If it's in YAML, GitOps owns it" | Git PR latency ≠ incident latency |
| "Small service doesn't need live config" | Small services join **shared incidents** |
| "Checklist slows teams" | One review hour saves **bridge hours** |

## The Aha

**Hot-path knobs are defined by read frequency, change trigger, and latency budget — not by team size.** If a value is read per request and changed during incidents, it belongs in a live tree with local `get*()` — not in constants, Redis poll, or GitOps YAML.

## Hot-path admission checklist

Score each proposed key. **YES on any of 1–4** strongly suggests Kiponos.

### Section A — Read pattern

| # | Question | YES → |
|---|----------|-------|
| A1 | Read on **every request** (or >100/sec per process)? | Hot path |
| A2 | Read inside **authorization, fraud, rate limit, or circuit** gate? | Hot path |
| A3 | Read in **tight loop** (streaming, inference batching)? | Hot path |
| A4 | **Java or Python** only (Kiponos SDK scope)? | Eligible |

### Section B — Change pattern

| # | Question | YES → |
|---|----------|-------|
| B1 | Changed during **incidents** or peaks? | Live ops |
| B2 | Changed in **seconds**, not PR + deploy? | Live ops |
| B3 | Owned by **SRE / fraud / FinOps**, not platform quarterly? | Live ops |
| B4 | Must **multiple services** see the same value quickly? | Live tree |

### Section C — Disqualifiers (do NOT put in Kiponos)

| # | Question | YES → use other tool |
|---|----------|---------------------|
| C1 | Secret (password, API key)? | **Vault** |
| C2 | Infrastructure topology (replicas, CRDs)? | **GitOps** |
| C3 | User cohort experiment? | **Feature flags** |
| C4 | Immutable legal/audit record? | **Compliance store** |
| C5 | Service stack is not Java or Python? | **Java/Python path** or wait — not Kiponos SDK today |

**Admit to Kiponos when:** A1–A4 and B1–B4 score YES, and C1–C5 are NO.

## Architecture

![Architecture diagram](https://files.catbox.moe/taij84.png)

## Worked example — `permits_per_second`

| Check | Result |
|-------|--------|
| A1 Read per request? | **YES** — rate limiter on every API call |
| A2 Security gate? | **YES** — abuse protection |
| B1 Incident change? | **YES** — DDoS throttle |
| B2 Seconds latency? | **YES** |
| C1 Secret? | NO |
| C2 Infra? | NO |
| **Verdict** | **Kiponos** — `limits/api/permits_per_second` |

## Worked example — `server.port`

| Check | Result |
|-------|--------|
| A1 Per request? | NO — bind at startup |
| B1 Incident? | NO |
| C2 Infra? | Borderline — **bootstrap YAML** |
| **Verdict** | **application.yml** / GitOps |

## Config tree — admitted keys for new microservice

```yaml
limits/
  api/
    permits_per_second: 800
    burst: 120
fraud/
  thresholds/
    block_score: 82
    review_score: 67
resilience/
  upstream_catalog/
    failure_rate_threshold: 40
    max_retries: 2
checklist/
  admitted_at: "2026-07-08"
  reviewer: platform-architecture
  rejected_to_gitops: ["server.port", "replicaCount"]
```

Profile path: `['platform']['prod']['checklist']` or per-service `['catalog']['api']['prod']['live']`.

## Integration — checklist-approved wiring

```java
@Service
public class RateLimitedApi {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();
    private final RateLimiter limiter;

    public RateLimitedApi(RateLimiter limiter) {
        this.limiter = limiter;
        kiponos.afterValueChanged(change -> {
            if (change.path().endsWith("permits_per_second")) {
                limiter.setRate(kiponos.path("limits", "api").getInt("permits_per_second"));
            }
        });
    }

    public Response handle(Request req) {
        int permits = kiponos.path("limits", "api").getInt("permits_per_second");
        if (!limiter.tryAcquire(permits)) {
            return Response.tooManyRequests();
        }
        return businessLogic(req);
    }
}
```

```python
import os
from kiponos import Kiponos

os.environ["KIPONOS_PROFILE"] = "['catalog']['api']['prod']['live']"
kiponos = Kiponos.create_for_current_team()

def block_score() -> int:
    return kiponos.path("fraud", "thresholds").get_int("block_score", 82)
```

## Real scenarios — checklist outcomes

| Key | Checklist verdict | Store |
|-----|-------------------|-------|
| `permits_per_second` | Hot + incident | **Kiponos** |
| `block_score` | Hot + fraud | **Kiponos** |
| `replicaCount` | C2 infra | **GitOps** |
| `db_password` | C1 secret | **Vault** |
| `new_ui_for_5pct_users` | C3 cohort | **LaunchDarkly** |
| `server.port` | Cold bootstrap | **YAML** |
| `saga.timeout_ms` | B4 cross-service | **Kiponos** |

## Performance — why checklist matters

- **Constants** — zero read cost, infinite change cost (deploy)
- **Redis poll 10s** — hot path sees stale `permits_per_second` during DDoS
- **GitOps PR** — minutes; checklist catches before wiring
- **Kiponos local read** — checklist-approved keys get microsecond lookup
- **Binder hooks** — `afterValueChanged` updates Resilience4j without restart

## Compare to alternatives

| Store | Hot-path read | Incident edit | Checklist fit |
|-------|---------------|---------------|---------------|
| `static final` | Fast | Deploy | **Fail** A+B |
| Redis poll | Poll staleness | Medium | **Fail** B2 |
| GitOps YAML | Static until deploy | Poor | **Fail** B2 |
| Feature flags | Network eval | Wrong semantics | **Fail** C3 |
| **Kiponos** | **Local get*()** | **Seconds** | **Pass** |

## When not to use Kiponos

| Checklist result | Action |
|------------------|--------|
| C1 secret | Vault |
| C2 infra | GitOps |
| C3 cohort | Feature flags |
| C4 immutable record | Compliance workflow |
| C5 non Java/Python | Document gap; do not fake SDK |
| Cold path bootstrap | YAML OK |

## Getting started (15 minutes)

1. Copy checklist sections A, B, C into design review template.
2. [TeamPro at kiponos.io](https://kiponos.io) — create service profile `['your-service']['prod']['live']`.
3. Run checklist on first five proposed keys.
4. Wire admitted keys only — reject `permits_per_second` in YAML.
5. Add `checklist/admitted_at` metadata key in tree for audit.
6. PR policy: no hot-path constant merges without architect sign-off.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [Ops knob taxonomy](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-ops-knob-taxonomy.md)
- [When GitOps, when live config checklist](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-buy-guide-when-gitops-when-live-config.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — run the checklist once at design review; skip the bridge debate on every incident afterward.*