---
title: "When Not to Use Live Config — Honest Boundaries (Architecture)"
published: false
tags: architecture, devops, java, python
description: Explicit anti-cases: secrets, infra desired state, cohort experiments, immutable audit records.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-when-not-to-use-live-config.md
main_image: https://files.catbox.moe/y1msbh.jpg
---

Vendor RFP. The prospect asks: *"Can Kiponos replace our secrets manager, GitOps pipeline, and experimentation platform?"* Sales wants to say yes. The principal architect pulls up this doc:

> "Kiponos is a **live operational config hub** — not everything that changes in production. State the **anti-cases** explicitly or we recreate sprawl in reverse."

Honest boundaries build trust. [Kiponos.io](https://kiponos.io) owns **operational knobs** with local Java and Python `get*()` on hot paths. It does **not** own secrets, infrastructure desired state, cohort experiments, or immutable compliance records. Use this pattern during **vendor RFP — state what Kiponos does not do**.

## The problem — hub scope creep

Teams migrate everything to the hub because the dashboard is convenient:

```yaml
# Anti-pattern — wrong classes in Kiponos tree
payments_live/
  db_password: "..."              # secret — WRONG
  replica_count: 12               # gitops — WRONG
  new_checkout_for_5pct_eu: true  # experiment — WRONG
  block_score: 82                 # ops — CORRECT
```

Scope creep **couples** secret rotation outages to fraud tuning and erodes security review.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "One dashboard to rule them all" | Mixed classes **break audit** boundaries |
| "Live config is always faster" | GitOps **correct** for infra topology |
| "We can put secrets in hub with ACL" | Not a secrets manager — no rotation HSM model |
| "Flags and ops are the same" | Cohort targeting ≠ circuit thresholds |
| "If it changes in prod, use Kiponos" | **Change trigger** and **read pattern** decide |

## The Aha

**Live config is for operational behavior on hot paths — not for every mutable value in production.** Saying no to anti-cases is as important as saying yes to `block_score`.

## Boundary matrix — what Kiponos does NOT do

| Anti-case | Why not Kiponos | Use instead |
|-----------|-----------------|-------------|
| **Secrets** | No envelope encryption rotation model | Vault, Secrets Manager |
| **Infra desired state** | Replicas, CRDs, IAM, LB ARNs | GitOps, Terraform |
| **Cohort experiments** | User targeting + exposure analytics | Statsig, LaunchDarkly |
| **Immutable audit records** | Hub is mutable by design | WORM store, SIEM |
| **Legal policy documents** | Not a compliance workflow | Wiki + GRC tool |
| **Bootstrap wiring** | Rarely changed; Git review valuable | Helm, YAML |
| **Non Java/Python hot paths** | SDK scope today | Java/Python service boundary |
| **CDN/WAF vendor rule packs** | Vendor-owned signatures | Cloudflare, AWS WAF |

## Architecture — hub in context

![Architecture diagram](https://files.catbox.moe/13h29t.png)

## Config tree — boundaries documented in profile

```yaml
boundaries/
  hub_owns:
    - fraud thresholds
    - circuit breaker floats
    - rate limits and tenant quotas
    - saga timeouts
    - ml batch and pool sizes
    - peak posture knobs
  hub_does_not_own:
    - db passwords and API keys
    - replica counts and CRDs
    - cohort experiment gates
    - PCI KEK material
    - TLS private keys
  rfp_version: "2026.1"
fraud/
  thresholds/
    block_score: 82
resilience/
  payments/
    failure_rate_threshold: 35
```

Profile path: `['platform']['prod']['boundaries']`.

## Integration — enforce boundaries in code (Java)

```java
@Service
public class BoundaryAwarePayments {
    private final Kiponos kiponos;
    @Value("${spring.datasource.password}")
    private final String dbPassword;  // Vault — NOT kiponos

    public BoundaryAwarePayments(Kiponos kiponos,
                                 @Value("${spring.datasource.password}") String dbPassword) {
        this.kiponos = kiponos;
        this.dbPassword = dbPassword;
    }

    public Decision authorize(int riskScore) {
        // YES — operational hot path
        int block = kiponos.path("fraud", "thresholds").getInt("block_score");
        if (riskScore >= block) return Decision.block();
        return Decision.approve();
    }

    public Connection dbConnection() {
        // NO — secret from Vault-injected env, never kiponos.path("db_password")
        return dataSource(dbPassword);
    }
}
```

```python
import os
from kiponos import Kiponos

os.environ["KIPONOS_PROFILE"] = "['platform']['prod']['boundaries']"
kiponos = Kiponos.create_for_current_team()

# YES
def block_score() -> int:
    return kiponos.path("fraud", "thresholds").get_int("block_score", 82)

# NO — use os.environ from Vault at worker start
DATABASE_URL = os.environ["DATABASE_URL"]
```

## Anti-case decision table — RFP copy-paste

| Prospect question | Honest answer |
|-------------------|---------------|
| Replace Vault? | **No** — secrets stay in Vault |
| Replace Argo CD? | **No** — infra desired state stays GitOps |
| Replace LaunchDarkly? | **No** for cohort experiments; optional bools only if consolidating |
| Replace Consul service discovery? | **No** — keep Consul for SD |
| Tune fraud thresholds live? | **Yes** |
| Share saga timeouts Java + Python? | **Yes** |
| Store PCI PAN? | **No** — never |
| Node.js SDK? | **Not supported** — Java/Python only |

## Real scenarios

| Event | Scope creep mistake | Honest boundary |
|-------|---------------------|-----------------|
| Vendor RFP | "Unified platform" overclaim | Boundary matrix in response |
| PM migrates experiment to hub | Wrong analytics | Redirect to Statsig |
| SRE puts replica count in hub | Couples to fraud edits | GitOps PR |
| Security audit | Password in tree | Block merge; Vault only |
| ML Node service | Demands Kiponos | Java/Python boundary doc |

## Performance — why boundaries matter

- **Secrets in hub** — wrong security model; if attempted, per-read encryption unusable at 11k TPS
- **Infra keys in hub** — bypasses Git audit trail for cluster topology
- **Experiments in hub** — missing exposure events; wrong evaluation semantics
- **Ops keys in Git** — PR latency on incidents
- **Clean boundary** — each tool optimized for its read/write pattern

## Compare to alternatives

| Need | Wrong tool | Right tool |
|------|------------|------------|
| Credential rotation | Kiponos | Vault |
| 5% EU canary | Kiponos alone | LaunchDarkly |
| `block_score` incident | GitOps | Kiponos |
| StatefulSet image | Kiponos | GitOps |
| Immutable SOC2 export | Kiponos history | WORM archive |

## When TO use Kiponos (contrast)

| Signal | Use Kiponos |
|--------|-------------|
| Read per request on Java/Python path | Yes |
| Change during incident in seconds | Yes |
| Nested fraud/resilience/limits tree | Yes |
| Cross-runtime shared ops values | Yes |

## Getting started (15 minutes) — publish boundaries

1. Create `['platform']['prod']['boundaries']` with `hub_owns` / `hub_does_not_own` lists.
2. Grep hub for `password`, `api_key`, `replica` — migrate out.
3. Add RFP appendix linking this pattern.
4. PR template: *"Confirm key not in anti-case list."*
5. Wire `authorize()` to `fraud/thresholds` only — secrets from `@Value` Vault injection.
6. Share [hot-path checklist](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-buy-guide-hot-path-config-checklist.md) with prospects.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [When Vault, when Kiponos](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-buy-guide-when-vault-when-kiponos.md)
- [Ops knob taxonomy](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-ops-knob-taxonomy.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — honest boundaries close RFPs faster than overclaiming a hub that was never meant to hold your DB password.*