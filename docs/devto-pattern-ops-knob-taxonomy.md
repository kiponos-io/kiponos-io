---
title: "Ops Knob Taxonomy — Classify Keys Before You Drown in Tools (Architecture)"
published: false
tags: architecture, devops, java, python
description: Taxonomy: bootstrap, operational, experiment, secret — where each lives.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-ops-knob-taxonomy.md
main_image: https://files.catbox.moe/id94bo.jpg
---

Config sprawl retro. Engineering counted **1,240 keys** across Helm, Vault paths, LaunchDarkly, Redis hashes, and `static final` constants. Nobody could answer which store owned `failure_rate_threshold`. The platform lead assigns homework:

> "Tag every key with a **knob class** before we buy another tool. I want a **taxonomy**, not another diagram."

This pattern defines four classes — **bootstrap**, **operational**, **experiment**, **secret** — and maps each to its home. [Kiponos.io](https://kiponos.io) owns **operational** knobs in nested trees with local Java/Python reads. Use during **config sprawl retro — tag every key by class**.

## The problem — untagged keys become political

```yaml
# Untagged sprawl
payments:
  block_score: 82           # which class?
  replicaCount: 12
  stripe_key: sk_live_xxx
  newCheckout: true
```

```java
private static final int BLOCK_SCORE = 90;  // operational — filed as "config"
```

Without taxonomy, incidents become tool debates. Without a **hub for operational class**, teams build Redis layer #4.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Teams know where keys belong" | Onboarding repeats **same arguments** |
| "Naming conventions are enough" | `prod_payments_flag_v2` does not imply **class** |
| "We will tag later" | Later is **during an incident** |
| "Taxonomy is bureaucracy" | Untagged keys cost **bridge hours** |
| "One tool can hold all classes" | Mixed classes **couple** rotation to fraud tuning |

## The Aha

**Knob class precedes tool choice.** Bootstrap declares wiring. Operational tunes runtime under load. Experiment targets cohorts. Secret proves identity. Tag first; then Vault, GitOps, feature flags, or Kiponos.

## Taxonomy — four classes

| Class | `knob_class` tag | Definition | Owner | Tool |
|-------|------------------|------------|-------|------|
| **bootstrap** | `bootstrap` | Wiring that changes quarterly via review | Platform | GitOps / YAML |
| **operational** | `ops` | Hot-path floats; incident/peak edits in seconds | SRE / fraud / FinOps | **Kiponos** |
| **experiment** | `experiment` | User cohort product behavior | Product / growth | Statsig / LD |
| **secret** | `secret` | Credentials and key material | Security | Vault |

## Architecture

![Architecture diagram](https://files.catbox.moe/yt99cu.png)

## Taxonomy registry tree — metadata pattern

Store classification **in the hub** for operational keys; use a **registry folder** for cross-team index:

```yaml
taxonomy/
  version: "2026.1"
  classes:
    bootstrap: "Git-reviewed wiring; rare change"
    ops: "Hot-path; incident latency seconds"
    experiment: "Cohort targeting"
    secret: "Vault only — never hub"
registry/
  payments.block_score:
    knob_class: ops
    home: kiponos
    path: fraud/thresholds/block_score
  payments.replicaCount:
    knob_class: bootstrap
    home: gitops
    path: helm/values-prod.yaml
  growth.new_checkout:
    knob_class: experiment
    home: statsig
    gate: new_checkout
  payments.db_password:
    knob_class: secret
    home: vault
    path: secret/data/payments/db
fraud/
  thresholds/
    block_score: 82
    knob_class: ops
resilience/
  payments/
    failure_rate_threshold: 35
    knob_class: ops
```

Profile path: `['platform']['prod']['taxonomy']`.

## Integration — class-aware reads (Java)

```java
@Service
public class TaxonomyAwareOps {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();

    public int blockScore() {
        // ops class — Kiponos hot path only
        assertOpsClass("payments.block_score");
        return kiponos.path("fraud", "thresholds").getInt("block_score");
    }

    private void assertOpsClass(String registryKey) {
        String cls = kiponos.path("registry", registryKey).getString("knob_class", "ops");
        if (!"ops".equals(cls)) {
            throw new IllegalStateException(
                registryKey + " is class " + cls + " — not readable on ops hot path");
        }
    }
}
```

### Python — ops class only on worker

```python
import os
from kiponos import Kiponos

os.environ["KIPONOS_PROFILE"] = "['platform']['prod']['taxonomy']"
kiponos = Kiponos.create_for_current_team()

def batch_size() -> int:
    return kiponos.path("ml", "routing").get_int("batch_size", 32)
```

## Classification worksheet — copy for retro

| Key | Read freq | Change trigger | Cohort? | Secret? | Class | Home |
|-----|-----------|------------------|---------|---------|-------|------|
| `block_score` | Per auth | Incident | No | No | **ops** | Kiponos |
| `replicaCount` | Deploy | Capacity plan | No | No | **bootstrap** | GitOps |
| `new_checkout` | Per session | Product | Yes | No | **experiment** | Statsig |
| `db_password` | Pool init | Rotation | No | Yes | **secret** | Vault |
| `failure_rate_threshold` | Per call | Incident | No | No | **ops** | Kiponos |
| `ingress.host` | Deploy | DNS | No | No | **bootstrap** | GitOps |

## Real scenarios

| Event | Without taxonomy | With taxonomy |
|-------|------------------|---------------|
| Config sprawl retro | 1,240 untagged keys | Registry populated in one sprint |
| PM puts `block_score` in Statsig | Accepted | Registry says **ops** → redirect |
| New hire adds float to Vault | Coupled rotation | **secret** vs **ops** gate in review |
| Incident edit | Wrong store latency | **ops** → dashboard seconds |
| Vendor RFP | "We need everything" | Taxonomy defines scope |

## Performance

- **Ops class on hot path** — Kiponos local read; taxonomy check is one optional registry read at startup
- **Misclassified secret in hub** — security incident; taxonomy prevents
- **Experiment class on auth path** — exposure logging cost; taxonomy blocks in design review
- **Bootstrap in Redis poll** — unnecessary staleness; taxonomy sends to GitOps
- **Registry in hub** — same WebSocket; no separate CMDB

## Compare to alternatives

| Approach | Enforces class boundaries | Hot-path ops | Onboarding clarity |
|----------|---------------------------|--------------|-------------------|
| Naming conventions only | Weak | Varies | Poor |
| Confluence wiki table | Stale | N/A | Medium |
| **Taxonomy + registry tree** | **Strong** | **Kiponos for ops** | **Strong** |
| Single monolithic store | No | Coupled | Poor |

## When not to use Kiponos

| Class | Home — not Kiponos |
|-------|-------------------|
| `secret` | Vault |
| `bootstrap` | GitOps |
| `experiment` | Feature flags |
| Registry metadata for gitops keys | Git + CODEOWNERS |
| Immutable audit PDF | Compliance archive |

## Getting started (15 minutes)

1. Create profile `['platform']['prod']['taxonomy']` with `taxonomy/`, `registry/`, and first **ops** subtree.
2. Export 50 highest-traffic keys from Helm + code; fill classification worksheet.
3. Tag registry entries with `knob_class` and `home`.
4. Migrate ten **ops** keys into `fraud/` and `resilience/` folders.
5. Add PR checklist: *"New key must declare knob_class."*
6. Publish internal RFC linking taxonomy to [hot-path checklist](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-buy-guide-hot-path-config-checklist.md).

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [When GitOps, when live config](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-buy-guide-when-gitops-when-live-config.md)
- [When not to use live config](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-when-not-to-use-live-config.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — tag the class first; the hub owns every key labeled ops.*