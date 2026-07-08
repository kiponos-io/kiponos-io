---
title: "When Vault, When Kiponos — Secrets vs Operational Config (Architecture)"
published: false
tags: architecture, devops, security, java
description: Honest decision guide: Vault for secrets, Kiponos for hot-path ops knobs. Java/Python boundary examples.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-buy-guide-when-vault-when-kiponos.md
main_image: https://files.catbox.moe/y1msbh.jpg
---

Security architecture review. The platform team presents a **single config platform** slide: HashiCorp Vault for everything — connection strings, API keys, fraud `block_score`, Hikari `maximum_pool_size`, and feature toggles. The CISO nods at centralization. The payments principal engineer raises a hand:

> "If I call Vault on every authorization for `block_score`, I add **network RTT** to a path that already runs **11k TPS**. Vault is for **secrets**. Fraud thresholds are **operational config** — different rotation, different audit, different read contract."

The room needs a **buyer-grade boundary** — not vendor religion. [HashiCorp Vault](https://www.vaultproject.io/) (or AWS Secrets Manager, GCP Secret Manager) owns **credentials and key material**. [Kiponos.io](https://kiponos.io) owns **operational knobs** Java and Python services read thousands of times per second with **local `get*()`**. This guide is for architects assigning keys during **architecture review — assign keys to secret vs ops classes**.

## The problem — secrets and ops knobs in one bucket

Teams conflate classes because both change in production:

```java
// Anti-pattern — fraud threshold fetched like a secret
@Service
public class AuthGate {
    public Decision authorize(int riskScore) {
        int block = Integer.parseInt(
            vaultClient.read("secret/fraud/block_score").getData().get("value"));
        return riskScore >= block ? Decision.block() : Decision.approve();
    }
}
```

```yaml
# Vault path sprawl — ops floats beside credentials
secret/
  data/
    payments/
      db_password: "..."
      stripe_api_key: "sk_live_..."
      block_score: "82"           # wrong class
      hikari_max_pool: "40"       # wrong class
```

Vault is engineered for **infrequent read, tight ACL, rotation workflows, and encryption at rest**. Operational thresholds change **during incidents** and must be readable on **every request** without HSM round-trips.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "One secrets store simplifies architecture" | Mixing classes **couples** secret rotation outages to fraud tuning |
| "Vault dynamic secrets cover app config" | Dynamic DB creds ≠ **per-request fraud floats** |
| "We cache Vault reads — same as live config" | Cache TTL creates **staleness** — the Black Friday fraud bug |
| "Ops knobs are sensitive — they belong in Vault" | Sensitivity ≠ **secret**. Thresholds need audit, not envelope encryption per read |
| "Kiponos replaces Vault" | **No** — Kiponos does not store DB passwords or API keys |

## The Aha

**Secrets prove identity and access. Operational config proves runtime behavior.** Vault answers *"what credential unlocks this resource?"* Kiponos answers *"what threshold should this request see right now?"* Assign every key to one class before picking tooling.

## What Kiponos.io is — and what Vault keeps

**Vault (or cloud secret manager):**

- Database passwords, API keys, TLS private keys, encryption keys
- Rotation policies, dynamic credentials, PKI
- Infrequent read pattern — startup, periodic refresh, on-demand lease

**Kiponos.io:**

- WebSocket hub → in-memory tree in Java/Python SDK
- Profile paths like `['payments']['prod']['live']`
- Local `getInt("block_score")` on hot path — zero network
- Dashboard ACL for fraud, SRE, ML ops — sub-second edits

Bootstrap wiring still comes from Git + Vault:

```yaml
# application.yml — bootstrap only
kiponos:
  team-id: ${KIPONOS_ID}
  access-key: ${KIPONOS_ACCESS}   # from Vault at startup
  profile-path: "['payments']['prod']['live']"
spring:
  datasource:
    password: ${vault:secret/data/payments/db#password}
```

The **Vault access key** for Kiponos is a secret. **`block_score`** inside the hub is not.

## Architecture

![Architecture diagram](https://files.catbox.moe/a07g3o.png)

## Decision table — where does this key live?

| Key example | Class | Tool | Read pattern |
|-------------|-------|------|--------------|
| `db_password` | Secret | **Vault** | Startup / pool refresh |
| `stripe_api_key` | Secret | **Vault** | Outbound API client init |
| `kiponos_access_key` | Secret | **Vault** → env | Process bootstrap |
| `fraud.block_score` | Operational | **Kiponos** | Every authorization |
| `hikari.maximum_pool_size` | Operational | **Kiponos** | Pool + binder |
| `resilience.failure_rate_threshold` | Operational | **Kiponos** | Every circuit check |
| `ingress.host` | GitOps desired state | **Helm / Argo** | Deploy reconcile |
| `ml.embedding.batch_size` | Operational | **Kiponos** | Every inference batch |

**Rule:** if losing the value **compromises identity or cryptography**, it is a secret. If it **tunes behavior under load** and changes during incidents, it is operational.

## Boundary examples — Java and Python

### Java — Vault at bootstrap, Kiponos on hot path

```java
@Configuration
public class PaymentsConfig {

    @Bean
    public Kiponos kiponos(
            @Value("${kiponos.team-id}") String teamId,
            @Value("${kiponos.access-key}") String accessKey) {
        // accessKey sourced from Vault Agent at pod start — not per request
        return Kiponos.builder()
            .teamId(teamId)
            .accessKey(accessKey)
            .profilePath("['payments']['prod']['live']")
            .build();
    }

    @Bean
    public DataSource dataSource(@Value("${spring.datasource.password}") String password) {
        // password from Vault — rotated on policy, not per HTTP request
        return DataSourceBuilder.create()
            .password(password)
            .build();
    }
}

@Service
public class FraudGate {
    private final Kiponos kiponos;

    public Decision evaluate(int riskScore) {
        int block = kiponos.path("fraud", "thresholds").getInt("block_score");
        if (riskScore >= block) return Decision.block();
        return Decision.approve();
    }
}
```

### Python — same boundary

```python
import os
from kiponos import Kiponos

# KIPONOS_ACCESS injected by Vault sidecar at worker start
os.environ["KIPONOS_PROFILE"] = "['inference']['prod']['live']"
kiponos = Kiponos.create_for_current_team()

def batch_size() -> int:
    return kiponos.path("ml", "embedding").get_int("batch_size", 32)

# DB password from env populated once at container start — not in Kiponos
DATABASE_URL = os.environ["DATABASE_URL"]
```

## Config tree — operational lane only

```yaml
fraud/
  thresholds/
    block_score: 82
    review_score: 67
    velocity_per_hour: 14
resilience/
  payments/
    failure_rate_threshold: 35
    wait_duration_open_ms: 22000
limits/
  tenant_acme/
    rpm: 6000
ml/
  embedding/
    batch_size: 32
    worker_pool_size: 24
# NO db_password, NO api keys — those stay in Vault
```

Profile path: `['payments']['prod']['live']`.

## Real scenarios — pick the right store

| Scenario | Wrong choice | Right choice |
|----------|--------------|--------------|
| Rotate Stripe API key quarterly | Kiponos dashboard | **Vault** rotation workflow |
| BIN attack — lower block score now | Vault PR + cache stale | **Kiponos** delta |
| New microservice needs DB cred | Kiponos tree | **Vault** dynamic secret |
| GPU OOM — shrink batch size | Secret store | **Kiponos** live |
| Audit: who changed fraud threshold | Vault audit log (wrong tool) | **Kiponos** hub actor log |
| Compliance: encrypt PAN key material | Kiponos | **Vault** HSM path |

## Performance — why class separation matters

- **Vault read** — milliseconds + policy check; wrong on 11k TPS authorization path
- **Vault cache** — trades staleness for throughput — unacceptable for incident knobs
- **Kiponos hot-path read** — in-process memory; nanoseconds beside risk scoring
- **Single bootstrap Vault lease** for Kiponos credentials — amortized at startup
- **Rotation isolation** — rotating DB password does not flush fraud tree cache

## Compare to alternatives

| Criterion | Vault / Secrets Manager | Kiponos | Verdict |
|-----------|-------------------------|---------|---------|
| Credential rotation | **Excellent** | Not supported | Vault |
| Per-request numeric threshold | Poor fit | **Excellent** | Kiponos |
| Encryption at rest for keys | **Excellent** | Hub ACL | Vault |
| Sub-second ops edit | Poor | **Excellent** | Kiponos |
| Nested ops trees | Awkward paths | **Native** | Kiponos |
| Hot-path local read | Cache with staleness | **SDK cache** | Kiponos |

## When not to use Kiponos

| Case | Use instead |
|------|-------------|
| Database passwords, API keys, signing keys | **Vault** |
| TLS private keys, mTLS client certs | **Vault / cert-manager** |
| PCI key encryption keys (KEK) | **HSM / Vault** |
| Bootstrap: Kiponos team access credential | **Vault** → env at start |
| Immutable legal hold documents | Compliance archive |

## Getting started (15 minutes) — classify keys in review

1. Export keys from Vault paths, Helm, and code constants.
2. Tag each: **secret** | **operational** | **gitops** | **product_flag**.
3. Move any `block_score`-style floats **out of Vault** into Kiponos profile `['payments']['prod']['live']`.
4. Keep `kiponos.access-key` in Vault — inject at pod start via Agent or CSI driver.
5. Wire Java `FraudGate` / Python `batch_size()` to Kiponos; verify Vault is **not** called per request.
6. Publish internal RFC: *"Vault for credentials. Kiponos for knobs."*

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [Hot-path config checklist](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-buy-guide-hot-path-config-checklist.md)
- [Ops knob taxonomy](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-ops-knob-taxonomy.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — Vault guards the keys. The live tree guards how hard production runs when fraud spikes.*