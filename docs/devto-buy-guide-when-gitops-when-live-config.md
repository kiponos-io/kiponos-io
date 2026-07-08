---
title: "When GitOps, When Live Config — Buyer Checklist (Architecture)"
published: false
tags: architecture, gitops, devops, java
description: Extends when-gitops article as buyer-facing checklist for platform leads.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-buy-guide-when-gitops-when-live-config.md
main_image: https://files.catbox.moe/y1msbh.jpg
---

Quarterly platform review. The whiteboard shows **four overlapping boxes**: Argo CD, Spring Cloud Config, a feature-flag SaaS, and a Redis hash "for operational floats." Every incident reopens the same argument — **which box owns the knob** that just caught fire.

The new platform lead asks:

> "We have GitOps. Why do we also need live config? Give me a **checklist** I can run on every new key before we drown in tools."

This article is the **buyer-facing companion** to [When GitOps, when live config, when feature flags](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-when-gitops-when-live-config.md). [Kiponos.io](https://kiponos.io) owns the **operational lane** — nested trees, WebSocket deltas, local Java and Python reads. GitOps owns **desired state**. Feature flags own **cohorts**. Use this checklist during **quarterly review — classify new keys before tooling**.

## The problem — config sprawl without classification

Without explicit boundaries, teams default to whichever tool was purchased first:

```yaml
# values-prod.yaml — mixed classes (anti-pattern)
replicaCount: 12
ingress:
  host: api.example.com
resilience4j:
  circuitbreaker:
    instances:
      payments:
        failureRateThreshold: 50   # operational — wrong home
features:
  newCheckout: true                # product — debatable
```

```java
private static final int FAILURE_RATE_THRESHOLD = 50;
```

Incident at 14:00: fraud needs `block_score` 90 → 82. Someone opens a GitOps PR. Product wants checkout canary 5% → 10%. Platform rotates TLS via cert-manager. **Three tools, three latencies, one bridge.**

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "GitOps covers all config" | Git reconciles **desired state**, not sub-minute ops knobs |
| "ConfigMaps are live enough" | Mount sync + rollout = **minutes** |
| "Flags hold floats" | Fraud thresholds are not **cohort experiments** |
| "One more Redis layer unifies" | Fourth system without dashboard ACL |
| "Diagrams in Confluence fix sprawl" | Without **per-key classification**, sprawl returns |

## The Aha

**Assign every key to a config class before assigning a tool.** GitOps reconciles cluster topology. Feature flags decide user-visible experiments. Kiponos holds operational floats every service reads locally — thresholds, limits, timeouts, saga coordination.

## Buyer checklist — seven questions per new key

Run this on every proposed configuration key:

| # | Question | If YES → |
|---|----------|----------|
| 1 | Does it declare **infrastructure topology** (replicas, CRDs, IAM)? | **GitOps** |
| 2 | Is it a **secret** (password, API key, signing key)? | **Vault** |
| 3 | Does it target **user cohorts** for product experiments? | **Feature flags** |
| 4 | Does it change because of **production behavior** (incident, load, fraud)? | **Live config (Kiponos)** |
| 5 | Is it read on a **hot path** thousands of times per second? | **Live config** — local read |
| 6 | Is change latency requirement **seconds**, not PR + reconcile? | **Live config** |
| 7 | Is it a **one-time bootstrap default** reviewed quarterly? | **GitOps** or static YAML OK |

If questions 4–6 are YES and 1–3 are NO → **Kiponos**.

## Architecture — three lanes

![Architecture diagram](https://files.catbox.moe/5by619.png)

## Decision matrix — expanded buyer table

| Key example | Lane | Tool | Change latency |
|-------------|------|------|----------------|
| `replicaCount: 12` | GitOps | Helm + Argo | PR + reconcile |
| `ingress.host` | GitOps | Kustomize | PR + reconcile |
| `fraud.block_score` | Live ops | **Kiponos** | Seconds |
| `failure_rate_threshold` | Live ops | **Kiponos** | Seconds |
| `hikari.maximum_pool_size` | Live ops | **Kiponos** | Seconds + binder |
| `saga.inventory.timeout_ms` | Live ops | **Kiponos** | Seconds |
| `new_checkout for 5% EU users` | Product | **LaunchDarkly** | Experiment cadence |
| `ml.embedding.batch_size` | Live ops | **Kiponos** | Seconds |
| TLS certificate | GitOps | cert-manager | Automated |
| API keys | Secrets | Vault | Rotation policy |
| `KIPONOS_PROFILE` bootstrap | GitOps + secret | Helm + Vault | Rare |

Profile path for operational lane: `['platform']['prod']['lanes']` — or per-service `['payments']['prod']['live']`.

## Config tree — operational lane example

```yaml
payments_ops/
  fraud/
    block_score: 82
    review_score: 68
    velocity_per_hour: 15
  resilience/
    payments/
      failure_rate_threshold: 35
      wait_duration_open_ms: 25000
  limits/
    tenant_acme/
      rpm: 7000
  saga/
    inventory/
      compensation_timeout_ms: 40000
  ml/
    routing/
      batch_size: 32
classification/
  last_review_quarter: "2026-Q2"
  keys_migrated_from_gitops: 12
```

GitOps `values-prod.yaml` retains only bootstrap:

```yaml
kiponos:
  profilePath: "['payments']['prod']['live']"
# resilience4j floats REMOVED — live in hub
```

## Integration — operational lane wiring

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

@Service
public class LaneAwarePayments {
    private final Kiponos kiponos;

    public Decision authorize(int riskScore) {
        int block = kiponos.path("payments_ops", "fraud").getInt("block_score");
        if (riskScore >= block) return Decision.block();

        float threshold = kiponos.path("payments_ops", "resilience", "payments")
            .getFloat("failure_rate_threshold");
        if (downstreamUnhealthy(threshold)) return Decision.degrade();
        return Decision.approve();
    }
}
```

```python
import os
from kiponos import Kiponos

os.environ["KIPONOS_PROFILE"] = "['payments']['prod']['live']"
kiponos = Kiponos.create_for_current_team()

def batch_size() -> int:
    return kiponos.path("payments_ops", "ml", "routing").get_int("batch_size", 64)
```

## Real scenarios — checklist in action

| Scenario | Wrong lane | Right lane |
|----------|------------|------------|
| Scale Deployment 10 → 20 | Kiponos | **GitOps** |
| BIN attack — lower block score | GitOps PR (27 min) | **Kiponos** (seconds) |
| 5% checkout UI canary for EU | Kiponos bool without targeting | **LaunchDarkly** |
| Add Redis StatefulSet | Kiponos | **GitOps** |
| Saga timeout during partner outage | ConfigMap rollout | **Kiponos** |
| Rotate DB password | Kiponos | **Vault** |
| New key: `permits_per_second` on API | Run checklist Q4–6 → YES | **Kiponos** |

## Performance — why lane separation matters

- **GitOps reconcile** — correct for infra; wrong for per-request floats
- **Feature-flag evaluation** — user context at 12k TPS for `block_score` is expensive
- **Kiponos read** — local cache; microsecond-scale
- **ConfigMap volume** — sync delay; restart culture
- **Checklist prevents fourth Redis layer** — sprawl has real latency cost

## Compare to alternatives

| Criterion | GitOps | Kiponos | Feature flags |
|-----------|--------|---------|---------------|
| Declarative infra | **Excellent** | No | No |
| Sub-second incident knob | Poor | **Excellent** | Poor fit |
| User cohort targeting | No | App logic | **Excellent** |
| Nested ops trees | YAML in Git | **Native** | JSON hacks |
| Hot-path local read | N/A | **SDK** | Network eval |
| Audit via Git blame | **Excellent** | Hub log | Experiment history |

## When not to use Kiponos

| Use case | Lane | Tool |
|----------|------|------|
| New microservice Deployment | GitOps | Argo CD |
| cert-manager Certificate | GitOps | Kubernetes |
| Multivariate UI experiment | Product | LaunchDarkly |
| Quarterly default wiring | GitOps | Git PR OK |
| Immutable compliance record | Legal | Wiki + workflow |

## Getting started (15 minutes) — run the checklist

1. Export keys from Helm, Config Server, flags, code constants.
2. Tag each: **gitops** | **live_ops** | **product_flag** | **secret**.
3. [TeamPro at kiponos.io](https://kiponos.io) — profile `['payments']['prod']['live']`.
4. Migrate **five highest-churn live_ops keys** first.
5. Publish internal doc: *"Git declares wiring; hub declares knobs; flags declare cohorts."*
6. Add checklist to PR template for any new `values-prod.yaml` key.
7. Game day: fraud spike — measure GitOps PR path vs hub path.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [When GitOps, when live config (framework)](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-when-gitops-when-live-config.md)
- [Hot-path config checklist](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-buy-guide-hot-path-config-checklist.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — GitOps for what you deploy. Flags for who sees it. The hub for how it runs when the bridge is loud.*