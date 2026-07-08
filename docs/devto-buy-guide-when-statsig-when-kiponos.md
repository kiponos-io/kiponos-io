---
title: "When Statsig, When Kiponos — Product Experiments vs Ops Thresholds (Architecture)"
published: false
tags: architecture, devops, java, python
description: Statsig for cohort experiments; Kiponos for fraud thresholds and pool sizes — honest boundaries.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-buy-guide-when-statsig-when-kiponos.md
main_image: https://files.catbox.moe/x854ey.jpg
---

Product platform standup. Growth celebrates a **checkout experiment** — Statsig gate `new_payment_sheet`, 8% EU exposure, funnel metrics wired. Payments SRE reports processor latency: need `failure_rate_threshold` at 28 and `fraud.block_score` at 79 before GPUs OOM on the embedding worker.

The growth lead:

> "Statsig has **dynamic config**. Put `block_score` in a config parameter — one SDK, one dashboard."

The principal engineer:

> "Statsig decides **which users see which product**. Our auth path runs **14k evaluations per second** for floats that change during incidents — not cohort bucketing with exposure logging."

[Statsig](https://statsig.com) excels at **gates, experiments, and product analytics**. [Kiponos.io](https://kiponos.io) excels at **fraud thresholds, circuit breakers, and pool sizes** with local `get*()` on money paths. This buyer guide answers **PM wants flag for block_score — redirect to ops tree**.

## The problem — experiment infrastructure on the authorization hot path

Correct Statsig usage — product gate:

```java
StatsigUser user = new StatsigUser.Builder()
    .setUserID(customerId)
    .build();
boolean showNewSheet = Statsig.checkGate(user, "new_payment_sheet");
```

Anti-pattern — ops float in experiment config:

```java
DynamicConfig resilience = Statsig.getConfig(user, "resilience_tuning");
int blockScore = resilience.getInt("block_score", 85);

if (riskScore >= blockScore) {
    return Decision.block();
}
```

Problems:

- **User context required** for system-bound thresholds — fake system users smell
- **Exposure logging** optimized for product analytics — overhead at 14k TPS
- **Namespace collision** — PM experiments and SRE incident keys in one console
- **Python workers + Java APIs** — duplicate dynamic config or custom sync

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Statsig dynamic config replaces a config hub" | Built for **parameter experiments**, not nested ops trees |
| "One SDK simplifies architecture" | Product and ops have **different latency and ownership** |
| "block_score is just another config param" | Fraud thresholds are **incident knobs**, not cohort tests |
| "We will namespace keys carefully" | Without RFC, PMs still file tickets for `block_score` gates |
| "Statsig + Redis for the rest" | Three systems for one platform |

## The Aha

**Statsig measures which product experiences win. Kiponos keeps production alive when processors degrade.** Keep `new_payment_sheet` in Statsig with full experiment analytics. Move `block_score`, `failure_rate_threshold`, and `worker_pool_size` to Kiponos — local reads, no per-transaction gate evaluation.

## What each platform owns

**Statsig:**

- Feature gates with cohort targeting and exposure events
- A/B experiments tied to warehouse metrics
- Dynamic config for **product-tunable** parameters (copy variants, default tabs)
- PM-owned release cadence

**Kiponos.io:**

- Profile `['payments']['api']['prod']['live']`
- Nested `fraud/thresholds/`, `resilience/payments/`, `ml/embedding/`
- WebSocket delta → Java/Python SDK memory
- SRE / fraud / ML ops dashboard ACL

## Architecture

![Architecture diagram](https://files.catbox.moe/a5tq1l.png)

## Decision table — Statsig vs Kiponos

| Key / behavior | Tool | Owner |
|----------------|------|-------|
| `new_payment_sheet` gate for 8% EU users | **Statsig** | Growth |
| Onboarding copy A/B test | **Statsig** | Product |
| `fraud.block_score` | **Kiponos** | Fraud ops |
| `failure_rate_threshold` | **Kiponos** | SRE |
| `ml.embedding.worker_pool_size` | **Kiponos** | ML ops |
| `checkout_button_color` experiment | **Statsig** | Design / growth |
| `limits.partner_checkout.rpm` | **Kiponos** | Partner ops |
| Optional product bool in hub | **Kiponos** | If consolidating SDK reads only |

## Boundary examples — hybrid Java service

```java
@Service
public class CheckoutOrchestrator {
    private final Kiponos kiponos;

    public CheckoutView render(String customerId, int riskScore) {
        // Product plane — Statsig
        StatsigUser user = new StatsigUser.Builder().setUserID(customerId).build();
        boolean newSheet = Statsig.checkGate(user, "new_payment_sheet");

        // Ops plane — Kiponos local read
        int block = kiponos.path("fraud", "thresholds").getInt("block_score");
        if (riskScore >= block) {
            throw new FraudBlockedException();
        }

        float failThreshold = kiponos.path("resilience", "payments")
            .getFloat("failure_rate_threshold");
        if (processorUnhealthy(failThreshold)) {
            return CheckoutView.degraded(newSheet);
        }
        return CheckoutView.normal(newSheet);
    }
}
```

### Python embedding worker

```python
import os
from kiponos import Kiponos

os.environ["KIPONOS_PROFILE"] = "['payments']['api']['prod']['live']"
kiponos = Kiponos.create_for_current_team()

def worker_pool_size() -> int:
    return kiponos.path("ml", "embedding").get_int("worker_pool_size", 24)

# Statsig not imported here — worker has no user cohort context
```

## Config tree — ops keys out of Statsig

```yaml
fraud/
  thresholds/
    block_score: 79
    review_score: 65
    velocity_per_hour: 18
    bin_attack_mode: true
resilience/
  payments/
    failure_rate_threshold: 28
    wait_duration_open_ms: 22000
ml/
  embedding/
    worker_pool_size: 24
    batch_size: 32
    oom_guard_enabled: true
statsig_bridge/
  new_payment_sheet_gate: statsig_owned
  onboarding_copy_experiment: statsig_owned
```

Profile path: `['payments']['api']['prod']['live']`.

## Real scenarios

| Scenario | Statsig alone | Kiponos |
|----------|---------------|---------|
| PM wants `block_score` gate for "experiment" | Wrong semantics; needs fake users | Redirect to `fraud/thresholds` |
| Processor outage — lower circuit threshold | Dynamic config poll + user context | `getFloat()` — seconds |
| 8% checkout UI canary | **Statsig** — correct | N/A |
| GPU OOM — shrink worker pool | No user context on worker | Live `worker_pool_size` |
| Namespace collision incident | PM gate name shadows ops key | Separate hubs by design |

## Performance — evaluation vs local read

- **Statsig gate check** — network + user hashing + exposure event — fine for product paths
- **14k auth TPS** — per-request dynamic config evaluation adds **measurable CPU and IO**
- **Kiponos `getInt()`** — in-process tree lookup — microseconds beside risk engine
- **Hybrid checkout** — one Statsig gate per page + two Kiponos reads — clean separation
- **Cross-runtime** — Java API and Python worker share Kiponos tree; Statsig duplicated per runtime

## Compare to alternatives

| Criterion | Statsig | Kiponos | Verdict |
|-----------|---------|---------|---------|
| Cohort targeting + analytics | **Excellent** | App logic only | Statsig |
| Incident float during outage | Poor fit | **Excellent** | Kiponos |
| Nested fraud/resilience tree | Flat namespaces | **Native** | Kiponos |
| Hot-path local read | SDK network eval | **Memory** | Kiponos |
| Product experiment history | **Excellent** | N/A | Statsig |
| Java + Python ops sharing | Varies | **Both SDKs** | Kiponos |

## When not to use Kiponos

| Case | Use instead |
|------|-------------|
| Multivariate UI experiment with funnel metrics | **Statsig** |
| Gate rollout 5% → 50% with exposure logging | **Statsig** |
| Product copy dynamic config | **Statsig** |
| Infrastructure replica counts | GitOps |
| Secrets | Vault |

## Getting started (15 minutes)

1. List Statsig dynamic config keys — mark **product** vs **misplaced ops**.
2. Migrate ops keys to Kiponos `['payments']['api']['prod']['live']`.
3. Add `statsig_bridge/` documentation keys listing what stays on Statsig.
4. Refactor authorization to `kiponos.path("fraud", "thresholds").getInt("block_score")`.
5. PM workflow: experiments file in Statsig; fraud tickets edit Kiponos — publish RACI.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [Kiponos vs Statsig (deep dive)](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-vs-statsig.md)
- [Ops knob taxonomy](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-ops-knob-taxonomy.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — Statsig learns what users prefer. The hub keeps authorization thresholds honest at 14k TPS.*