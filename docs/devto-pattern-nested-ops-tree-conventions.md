---
title: "Nested Ops Tree Conventions — Folder Layout That Scales (Architecture)"
published: false
tags: architecture, devops, java, python
description: Conventions for fraud/resilience/ML folders — platform engineering pattern.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-nested-ops-tree-conventions.md
main_image: https://files.catbox.moe/id94bo.jpg
---

New team onboarding. Payments has `fraud/block_score` in Kiponos. Catalog invented `limits/catalog_rpm`. ML put `batchSize` at the profile root. Platform cannot grep one incident subtree across services.

The platform engineering RFC meeting:

> "We need **folder conventions** — fraud, resilience, limits, saga, ml — so every team nests ops keys the same way. `max_depth` and naming rules are not bureaucracy; they are **on-call ergonomics**."

This pattern defines **nested ops tree layout** for [Kiponos.io](https://kiponos.io) profiles. Adopt during **new team onboard — adopt tree layout RFC**.

## The problem — flat and divergent trees

```java
// Inconsistent paths across services
kiponos.path("block_score").getInt("value");  // catalog — wrong
kiponos.path("fraud").getInt("block_score");    // payments — better
kiponos.path("FraudThresholds").getInt("BlockScore");  // ML — chaos
```

Consul-era flat keys:

```
config/payments/prod/fraud/block_score
config/catalog/prod/catalog_rpm_limit
```

Without conventions, **cross-service incidents** require archaeology.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Teams self-organize folders" | Six teams → six hierarchies |
| "Flat keys are grep-friendly" | Flat keys **collide** at scale |
| "Naming is internal docs" | On-call uses **dashboard tree** under stress |
| "Depth does not matter" | Deep chaos vs **shallow standard** differs at 3 AM |
| "Conventions slow delivery" | Divergence slows **every** shared incident |

## The Aha

**Folder layout is an operational API.** Standard top-level domains — `fraud`, `resilience`, `limits`, `saga`, `ml`, `peak` — let any engineer navigate any service profile without a wiki. Conventions beat cleverness.

## Convention — top-level domains

| Folder | Contents | Example keys |
|--------|----------|--------------|
| `fraud/` | Scores, velocity, BIN attack | `thresholds/block_score` |
| `resilience/` | Circuit breakers, retries, bulkheads | `payments/failure_rate_threshold` |
| `limits/` | Rate limits, tenant quotas | `tenant_acme/rpm` |
| `saga/` | Compensation timeouts, signals | `inventory_compensation/timeout_ms` |
| `ml/` | Batch size, pool, routing | `embedding/worker_pool_size` |
| `peak/` | Posture mode, peak overrides | `mode_enabled` |
| `audit/` | Flip metadata, last editor | `last_change_by` |

**Max depth:** 4 levels from profile root (e.g. `fraud/thresholds/block_score`). Deeper → refactor subdomain.

**Key naming:** `snake_case` only. No camelCase in tree.

## Architecture

![Architecture diagram](https://files.catbox.moe/bfgv85.png)

## Reference tree — payments service layout

```yaml
fraud/
  thresholds/
    block_score: 82
    review_score: 67
    velocity_per_hour: 14
resilience/
  payments/
    failure_rate_threshold: 35
    wait_duration_open_ms: 25000
    max_retries: 3
  inventory/
    failure_rate_threshold: 42
limits/
  global/
    rpm: 8000
  tenant_acme/
    rpm: 6000
    burst: 900
saga/
  inventory_compensation/
    timeout_ms: 45000
    max_retries: 3
  shipping_handoff/
    timeout_ms: 30000
    signal_name: ready_for_pickup
ml/
  routing/
    primary_model: v3
    fallback_model: v2
    batch_size: 32
conventions/
  layout_version: "2026.1"
  max_depth: 4
  owner_team: platform-engineering
```

Profile path: `['conventions']['prod']['layout']` — or per service `['payments']['prod']['live']`.

## Integration — path helper enforces conventions (Java)

```java
@Component
public class OpsTreePaths {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();
    private static final int MAX_DEPTH = 4;

    public int fraudBlockScore() {
        return kiponos.path("fraud", "thresholds").getInt("block_score");
    }

    public float paymentsCircuitThreshold() {
        return kiponos.path("resilience", "payments").getFloat("failure_rate_threshold");
    }

    public int tenantRpm(String tenantSlug) {
        validateDepth("limits", tenantSlug, "rpm");
        return kiponos.path("limits", tenantSlug).getInt("rpm");
    }

    private void validateDepth(String... segments) {
        if (segments.length > MAX_DEPTH) {
            throw new IllegalArgumentException(
                "Path exceeds max_depth " + MAX_DEPTH + ": " + String.join("/", segments));
        }
    }
}
```

### Python — same conventions

```python
import os
from kiponos import Kiponos

os.environ["KIPONOS_PROFILE"] = "['payments']['prod']['live']"
kiponos = Kiponos.create_for_current_team()

def saga_inventory_timeout_ms() -> int:
    return kiponos.path("saga", "inventory_compensation").get_int("timeout_ms", 45000)
```

## Layout RFC checklist — new team onboard

| Step | Action |
|------|--------|
| 1 | Copy reference tree skeleton |
| 2 | Map service-specific upstreams under `resilience/{upstream}/` |
| 3 | Place tenant limits under `limits/tenant_{slug}/` |
| 4 | Add `conventions/layout_version` key |
| 5 | Code review rejects root-level ops floats |
| 6 | Dashboard ACL by folder (fraud team → `fraud/*`) |

## Real scenarios

| Event | Ad-hoc layout | Conventions |
|-------|---------------|-------------|
| Cross-service incident | Six grep patterns | `resilience/*/failure_rate_threshold` |
| New team onboard | Reinvent folders | Clone RFC skeleton |
| Fraud handoff | Keys at root | `fraud/thresholds/*` |
| ML ops pool cut | `batchSize` typo | `ml/embedding/worker_pool_size` |
| Platform audit | Inconsistent depth | `max_depth` enforced in CI |

## Performance

- **Nested paths are O(1) SDK lookup** — depth does not add network hops
- **Shallow standard domains** — faster human navigation in dashboard than flat 200 keys
- **`OpsTreePaths` wrapper** — compile-time method names; tree stays flexible
- **Shared layout across Java/Python** — same strings in both SDKs
- **ACL by folder prefix** — security without per-key IAM sprawl

## Compare to alternatives

| Approach | Cross-team consistency | On-call grep | Dashboard UX |
|----------|------------------------|--------------|--------------|
| Flat KV keys | Poor | Key name only | Long list |
| Per-team custom | Divergent | Painful | Confusing |
| **RFC folder domains** | **Strong** | **Predictable** | **Tree navigation** |

## When not to use nested ops trees in Kiponos

| Case | Home |
|------|------|
| Secrets | Vault — never nested in hub |
| Infra replica counts | GitOps |
| Product experiment flags | Feature-flag tool |
| Single bootstrap URL | `application.yml` |

## Getting started (15 minutes)

1. Publish internal RFC `layout_version: 2026.1` with domain table above.
2. Create profile with `conventions/` metadata folder.
3. Refactor three root-level floats into `fraud/thresholds/` or `resilience/`.
4. Add `OpsTreePaths` (or Python module constants for path strings).
5. PR linter: reject new ops keys at profile root.
6. Link RFC from [ops knob taxonomy](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-ops-knob-taxonomy.md).

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [Microservices collaboration tree](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-microservices-collaboration.md)
- [Ops knob taxonomy](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-ops-knob-taxonomy.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — conventions turn nested trees into a shared language fraud and catalog teams both read at 3 AM.*