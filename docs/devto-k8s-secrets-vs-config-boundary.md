---
title: "Kubernetes Secrets vs Live Config — Where to Draw the Boundary (Kiponos Architecture Guide)"
published: false
tags: architecture, kubernetes, security, devops
description: Teams stuff JDBC URLs and rate limits into the same SealedSecret. Platform architecture needs a clear line — K8s holds credentials; Kiponos holds operational knobs Java and Python pods read locally.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-secrets-vs-config-boundary.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-k8s-secrets-vs-config-boundary.jpg
---

Friday 16:00 UTC. Security audit flags Deployment `payments-api`: **47 keys** in one SealedSecret, rotated quarterly "as a bundle." Half the values are not secrets — `RATE_LIMIT_RPM`, `FRAUD_BLOCK_SCORE`, `PARTNER_TIMEOUT_MS`. The other half are **actual credentials** mixed with config because "one Secret is easier than two systems."

Compliance asks for **least-privilege Secret access**. SRE wants to lower `pool_max` during an incident **without** a Reloader restart. Platform is stuck: splitting the Secret means a Helm refactor; keeping it means auditors keep frowning and incidents keep waiting on rollouts.

The staff engineer summarizes:

> "We never decided what belongs in **etcd secrets** versus what belongs in **ops' live toolbox**."

That decision is architecture — not a linter rule. [Kiponos.io](https://kiponos.io) is the **operational half**: behavioral knobs Java and Python services read via SDK with zero-latency local cache. Kubernetes Secrets stay the **credential half**: tokens, keys, certs — mounted once, rotated on PKI schedule.

**The Aha:** two env vars (`KIPONOS_ID`, `KIPONOS_ACCESS`) replace forty ConfigMap keys; **everything ops tunes weekly** moves to the hub; **everything that proves identity** stays in K8s Secrets.

## The problem — the everything Secret

```yaml
# anti-pattern: payments-api-secrets (SealedSecret)
stringData:
  DATABASE_URL: postgres://payuser:****@rds.example/payments
  STRIPE_API_KEY: sk_live_****
  FRAUD_BLOCK_SCORE: "85"
  PARTNER_READ_TIMEOUT_MS: "3000"
  POOL_MAX: "40"
  FEATURE_NEW_CHECKOUT: "true"
```

```yaml
# deployment — envFrom loads the kitchen sink
envFrom:
  - secretRef:
      name: payments-api-secrets
```

Pain:

1. **RBAC** — any pod SA with Secret read sees fraud thresholds and DB URLs alike
2. **Rotation** — changing `pool_max` triggers same ceremony as rotating API keys
3. **Audit** — SealedSecret git history does not show **who** changed block score at 3am
4. **Restart culture** — env vars are immutable without pod recycle

| What teams believe | What production does |
|--------------------|---------------------|
| "Secrets are encrypted at rest — good enough" | Non-secrets should not be in Secrets |
| "ConfigMaps for the rest" | ConfigMaps still need restart for live ops |
| "External Secrets Operator syncs all" | Sync lag; still one blob |
| "GitOps one file per service" | Ops opens emergency PRs anyway |

## What Kiponos.io is — operational config plane

[Kiponos.io](https://kiponos.io) is a **live config hub** with **Java** and **Python** SDKs. Each pod connects via WebSocket, holds an in-memory tree, serves `get*()` on the hot path with **no network**.

Profile convention:

```
['payments']['v3']['prod']['live']
```

**Not in Kiponos:** Stripe secret key, RDS password, TLS private keys, `KIPONOS_ACCESS` itself.

**In Kiponos:** fraud thresholds, pool sizes, partner timeouts, feature flags, rate limits — keys that change **hourly** during operations.

See also: [pods without ConfigMaps](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-no-configmaps.md), [GitOps vs live config](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-gitops-vs-live-config.md).

## Architecture — credential plane vs behavior plane

```mermaid
flowchart TB
    subgraph K8s["Kubernetes — credential plane"]
        SEC["Secret kiponos-auth<br/>KIPONOS_ID + KIPONOS_ACCESS"]
        PKI["Secret stripe-api-key<br/>partner mTLS certs"]
    end
    subgraph Hub["Kiponos — behavior plane"]
        TREE["payments/v3/prod/live<br/>thresholds pools timeouts"]
    end
    OPS["Platform ops dashboard"] -->|WebSocket deltas| TREE
    SEC --> JPOD["Java payments pod<br/>SDK"]
    PKI --> JPOD
    TREE --> JPOD
    SEC --> PPOD["Python webhook pod<br/>SDK"]
    TREE --> PPOD
```

## Boundary table — decide in your platform RFC

| Data | Kubernetes Secret | Kiponos hub | GitOps manifest |
|------|-------------------|-------------|-----------------|
| Kiponos team JWE tokens | Yes | No | Bootstrap only |
| Payment processor API key | Yes | No | SealedSecret |
| JDBC password | Yes | No | ESO / Vault |
| JDBC URL (non-prod emergency) | Prefer hub | **Yes** | Seed skeleton |
| `pool_max`, `read_timeout_ms` | **No** | **Yes** | No |
| `fraud/block_score` | **No** | **Yes** | No |
| Deployment `replicas`, Ingress host | No | No | **Yes** |
| HPA `maxReplicas` ceiling | No | No | **Yes** |
| HPA target utilization (ops) | No | **Yes** | Optional bootstrap |

Rule of thumb: if ops would change it **during an incident without a security review**, it belongs in Kiponos — not in a Secret.

## Config tree (behavior only — no credentials)

```yaml
fraud/
  thresholds/
    block_score: 85
    review_score: 70
    velocity_per_hour: 12
data/
  postgres/
    jdbc_url: jdbc:postgresql://rds.example:5432/payments
    pool_max: 40
    pool_min: 5
    query_timeout_ms: 8000
partners/
  stripe/
    read_timeout_ms: 5000
    max_connections: 80
  adyen/
    read_timeout_ms: 5000
limits/
  default_rpm: 1200
  tenant_override_enabled: true
features/
  new_checkout_enabled: true
  degraded_mode: false
```

Passwords and `sk_live_*` keys **never** appear in this tree.

## Java integration — minimal Secret surface

```yaml
# deployment.yaml — after boundary split
env:
  - name: KIPONOS_ID
    valueFrom:
      secretKeyRef:
        name: kiponos-auth
        key: id
  - name: KIPONOS_ACCESS
    valueFrom:
      secretKeyRef:
        name: kiponos-auth
        key: access
  - name: STRIPE_API_KEY
    valueFrom:
      secretKeyRef:
        name: stripe-credentials
        key: api_key
args:
  - "-Dkiponos=['payments']['v3']['prod']['live']"
```

```java
@Service
public class PaymentRouter {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();

    @Value("${STRIPE_API_KEY}")
    private String stripeApiKey;  // Secret — env once per pod lifetime

    public RouteDecision route(Transaction txn, int riskScore) {
        int block = kiponos.path("fraud", "thresholds").getInt("block_score");
        if (riskScore >= block) {
            return RouteDecision.block("score_exceeded");
        }
        int timeout = kiponos.path("partners", "stripe").getInt("read_timeout_ms");
        return chargeStripe(txn, stripeApiKey, timeout);
    }
}
```

## Python integration — same boundary

```python
import os
from kiponos import Kiponos

kiponos = Kiponos.create_for_current_team()
# Profile: ['webhooks']['prod']['live']

STRIPE_API_KEY = os.environ["STRIPE_API_KEY"]  # K8s Secret only


def should_accept_webhook(tenant_id: str) -> bool:
    rpm = kiponos.path("limits", tenant_id).get_int("rpm", 600)
    return tenant_bucket(tenant_id).try_acquire(rpm)


def verify_stripe_signature(payload: bytes, sig: str) -> bool:
    return stripe.Webhook.construct_event(payload, sig, STRIPE_API_KEY)
```

Credentials: **env from Secret**. Limits: **local Kiponos read**.

## Real scenarios

| Scenario | Everything Secret | Split boundary |
|----------|-------------------|----------------|
| Lower fraud block score 3am | Unseal + rollout | Dashboard delta |
| Rotate Stripe API key | Same Secret as pool_max | Rotate `stripe-credentials` only |
| SOC2 least-privilege | App SA reads 47 keys | SA reads 2 Kiponos tokens + 1 processor key |
| Staging points at new RDS | Duplicate SealedSecret | Edit `data/postgres/jdbc_url` in staging profile |
| Auditor asks who changed threshold | Git blame on SealedSecret | Hub change log with actor |

## Performance

- **Secrets:** mounted once; Kubernetes injects env at pod start — no hot-path cost
- **Kiponos:** O(1) local reads; one WebSocket per pod — not a Secret watch per key
- Splitting reduces **CSI / ESO sync** fan-out — fewer large Secret objects in etcd
- Incident tuning avoids **rolling restart** — no Secret version bump for `pool_max`
- HPA new pods: credentials from Secret, behavior from hub snapshot ([per-pod SDK](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-sdk-per-pod.md))

## Compare to alternatives

| Approach | Live ops without restart | Least-privilege Secrets |
|----------|-------------------------|-------------------------|
| One mega Secret | No | Poor |
| ConfigMap for non-secrets | No (Reloader) | Better RBAC, still restart |
| Vault dynamic secrets only | N/A for thresholds | Good credentials, no live ops |
| Spring Cloud Config | Poll / refresh | Credentials should not live there |
| **K8s Secrets + Kiponos SDK** | **Yes** | **Small Secret objects** |

## When not to use Kiponos (keep in Secrets / Git)

| Case | Home |
|------|------|
| Database passwords, API secret keys | K8s Secret + rotation pipeline |
| TLS private keys | cert-manager / PKI |
| `KIPONOS_ACCESS` token | K8s Secret — bootstrap credential |
| Ingress hostname, Deployment image tag | GitOps |
| PCI-mandated config-only-in-Git | Document exception process |

## Getting started (15 minutes)

1. Inventory one service's SealedSecret — tag each key: **credential** vs **operational**.
2. [TeamPro at kiponos.io](https://kiponos.io) — create profile `['payments']['v3']['prod']['live']`.
3. Migrate **operational** keys to hub; leave **credential** keys in dedicated Secrets.
4. Add SDK to Java or Python Deployment; reduce `envFrom` to token + processor secrets.
5. Run game day: rotate Stripe key (Secret only) and lower `block_score` (hub only) — confirm **only one** triggers rollout.
6. Publish platform RFC: *"Secrets prove identity; Kiponos steers behavior."*

**Further reading:**

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [No ConfigMaps pattern](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-no-configmaps.md)
- [Config without restart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-no-restart.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — Kubernetes keeps secrets secret; the hub keeps operations operational.*