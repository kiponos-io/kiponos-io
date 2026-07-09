---
title: "Kiponos vs HashiCorp Boundary — Platform Toolbox: Who Reaches Hosts vs How Services Behave (Architecture)"
published: false
tags: architecture, devops, security, java
description: HashiCorp Boundary excels at identity-based secure access to SSH, RDP, and database targets. Kiponos excels at live operational knobs — timeouts, pools, rate limits — once services are running. Honest platform toolbox comparison.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-vs-hashicorp-boundary.md
main_image: https://files.catbox.moe/x854ey.jpg
---

Sunday 02:17. Payments is red. The on-call SRE authenticates through **HashiCorp Boundary**, gets a scoped session to the `payments-api` bastion, and SSHs into the pod to confirm thread pools are exhausted. Access worked perfectly — identity verified, session recorded, target scoped to `prod-payments-eks`.

Inside the JVM, the fix is not another SSH hop. The circuit breaker still opens at `failure_rate_threshold: 50` baked into `application-prod.yml`, Hikari `maximum_pool_size` is still **40**, and the Python fraud worker still scores at `block_score: 85` from a module constant. Boundary got the engineer **to** the host. It cannot change **how the service behaves** while traffic flows.

The platform architect frames it in the postmortem:

> "Boundary answers **who can reach which infrastructure**. We still need a plane for **how services behave once they are running** — without `kubectl edit` or redeploy during the incident."

[HashiCorp Boundary](https://www.hashicorp.com/products/boundary) is excellent for **identity-based secure access** — SSH, RDP, database sessions with audit trails and least-privilege targets. [Kiponos.io](https://kiponos.io) is excellent for **live operational configuration** — circuit thresholds, pool sizes, rate limits, saga timeouts — with **WebSocket deltas** and **local hot-path reads** in Java and Python. **Kiponos does not replace Boundary.** They occupy different shelves in the **platform toolbox**.

## The problem — access control mistaken for runtime configuration

Typical Boundary integration for incident response:

```bash
# SRE authenticates via OIDC → Boundary broker → scoped SSH session
boundary connect ssh -target-id ttcp_payments_api_prod \
  -username oncall-sre@corp.example
```

```bash
# Inside the pod — the knobs are still frozen
$ grep failureRateThreshold /proc/1/environ   # not there
$ curl -s localhost:8080/actuator/env | jq '.propertySources[].properties."resilience4j..."'
# still 50 — changing it means ConfigMap PR or JVM restart
```

Boundary solves the **bastion problem** — no shared SSH keys, session TTL, identity audit. Teams then hit the second problem:

- **Circuit thresholds** need lowering while authorization holds at 9k TPS — SSH access does not change JVM floats
- **Connection pool sizes** must shrink to protect Postgres — `kubectl edit` is not an ops contract
- **Python fraud workers** on EC2 and **Java APIs** on EKS need aligned `block_score` — Boundary sessions do not sync config
- **Saga timeout_ms** across three services — no Boundary target for cross-service operational trees

Boundary is not wrong. **Runtime service behavior** is simply outside its threat model and product scope.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Boundary is our platform access layer" | Boundary is **session access** — not application config |
| "SSH in, fix the config, SSH out" | Editing files in a running pod **breaks GitOps** and does not propagate to siblings |
| "Vault + Boundary covers platform security" | Security stack covers **secrets and access** — not live circuit thresholds |
| "We will use Boundary for everything platform" | Access and **operational floats** are different problems |
| "Incident fix = someone with shell access" | Shell access without a **config hub** means manual, non-reproducible tweaks |

## The Aha

**HashiCorp Boundary controls WHO can reach WHICH host — SSH, RDP, database targets — with identity and audit. Kiponos controls HOW services behave once running — timeouts, pools, rate limits, fraud thresholds — with local SDK reads and no restart.** When Boundary, when Kiponos, when both: Boundary for **gated infrastructure access**; Kiponos for **live service knobs**; both during incidents where engineers need **secure shell** and **dashboard deltas** in parallel.

## What Kiponos.io is alongside Boundary

Kiponos is a real-time configuration hub. SDK connects via WebSocket to `wss://kiponos.io/api/io-kiponos-sdk`, loads profile `['payments']['platform']['prod']['live']`, holds values in memory. Dashboard edit → delta → next `getInt()` sees it — **no SSH session, no pod recycle, no Boundary broker hop on the request path**.

Boundary still owns target catalogs, credential brokering, and session recording. Everything under `payments_ops/` is hub-native — the layer Boundary was never designed to hold.

Vault holds **secrets**. Boundary holds **access paths**. Kiponos holds **operational parameters** both Java pods and Python workers read locally.

## Architecture — Boundary access plane vs Kiponos behavior plane

![Architecture diagram](https://files.catbox.moe/ncuiai.png)

Boundary gets engineers **to** the target. Kiponos changes **behavior across all replicas** without shell on each pod.

## Config tree — operational knobs Boundary does not carry

```yaml
payments_ops/
  resilience/
    partner/
      failure_rate_threshold: 35
      wait_duration_open_ms: 20000
      permitted_calls_half_open: 8
    inventory/
      failure_rate_threshold: 40
      wait_duration_open_ms: 35000
  runtime/
    hikari/
      maximum_pool_size: 28
      minimum_idle: 8
      connection_timeout_ms: 5000
  limits/
    default/
      rpm: 1500
    tenant_mega_corp/
      rpm: 9000
      burst: 1500
  fraud/
    block_score: 82
    review_score: 67
    velocity_per_hour: 14
  saga/
    inventory_compensation/
      timeout_ms: 45000
      max_retries: 3
```

## Java integration — live knobs after Boundary gets you in

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
```

```java
@Service
public class PartnerPaymentService {

    private final Kiponos kiponos;
    private final CircuitBreakerRegistry breakers;

    public PartnerPaymentService(Kiponos kiponos, CircuitBreakerRegistry breakers) {
        this.kiponos = kiponos;
        this.breakers = breakers;
        kiponos.afterValueChanged(change -> {
            if (change.path().startsWith("payments_ops/resilience/partner")) {
                rebindPartnerCircuit();
            }
            if (change.path().startsWith("payments_ops/runtime/hikari")) {
                resizeHikariPool();
            }
        });
    }

    public PaymentResult authorize(PaymentRequest request) {
        CircuitBreaker partner = breakers.circuitBreaker("partner");
        return partner.executeSupplier(() -> callPartner(request));
    }

    private void rebindPartnerCircuit() {
        int threshold = kiponos.path("payments_ops", "resilience", "partner")
                .getInt("failure_rate_threshold", 50);
        long waitMs = kiponos.path("payments_ops", "resilience", "partner")
                .getLong("wait_duration_open_ms", 30000);
        breakers.circuitBreaker("partner").reset();
        // apply live Resilience4j config from hub values
        log.warn("partner circuit rebound: threshold={} waitMs={}", threshold, waitMs);
    }
}
```

The SRE may still Boundary-SSH to **inspect** thread dumps. The **threshold change** happens in Kiponos — every pod picks it up, not just the one shell session.

## Python integration — fraud worker shares the ops tree

```python
import os
from kiponos import Kiponos

os.environ["KIPONOS_PROFILE"] = "['payments']['platform']['prod']['live']"
kiponos = Kiponos.create_for_current_team()

def evaluate_risk(score: int, txn_count_last_hour: int) -> str:
    block = kiponos.path("payments_ops", "fraud").get_int("block_score", 85)
    velocity_limit = kiponos.path("payments_ops", "fraud").get_int("velocity_per_hour", 12)
    if txn_count_last_hour > velocity_limit:
        return "block"
    if score >= block:
        return "block"
    review = kiponos.path("payments_ops", "fraud").get_int("review_score", 70)
    if score >= review:
        return "review"
    return "allow"
```

Boundary can broker access to the EC2 host running this worker. It cannot align `block_score` with Java authorization pods — Kiponos can.

## Real scenarios — when Boundary, when Kiponos, when both

| Event | Boundary alone | Kiponos alone | Both (recommended) |
|-------|----------------|---------------|-------------------|
| On-call needs SSH to payments pod | **Scoped session + audit** | No shell access | Boundary for access |
| Processor brownout — lower circuit threshold | SSH does not change JVM floats | `resilience/partner/failure_rate_threshold` live | Boundary to diagnose; Kiponos to fix |
| DBA needs temporary DB console | **Brokered DB session** | Not a database proxy | Boundary for DB access |
| BIN attack — lower fraud block score | Manual edit on one host | `fraud/block_score` all replicas | Kiponos for fix; Boundary if shell needed |
| Thread pool exhaustion — shrink Hikari | Read-only inspection via SSH | `runtime/hikari/maximum_pool_size` live | Both — inspect + tune |
| Contractor offboarding — revoke infra access | **Disable Boundary role** | Unrelated — ops keys stay governed | Boundary for identity lifecycle |

## Performance — access sessions vs hot-path config reads

- **Boundary session setup** — OIDC + broker handshake — seconds; **not per HTTP request**
- **Kiponos read** — in-process tree lookup on every `authorize()` and fraud evaluation
- **SSH config edit** — affects one pod, breaks replica consistency, no SDK contract
- **Dashboard delta** — one key change reaches **all** Java and Python processes on the profile
- **Audit models differ** — Boundary logs **who connected where**; Kiponos logs **who changed which ops key**
- **Incident parallelism** — engineer holds Boundary session for logs while teammate edits hub — no queue for shell access to propagate floats

## Honest comparison table

| Criterion | HashiCorp Boundary | Kiponos | Honest verdict |
|-----------|-------------------|---------|----------------|
| Identity-based SSH / RDP / DB access | **Core strength** | Not an access broker | Boundary for infrastructure sessions |
| Session audit and least-privilege targets | **Native** | Out of scope | Boundary for compliance access trail |
| Live circuit breaker thresholds | Not in product scope | **Dashboard delta** | Kiponos for resilience floats |
| Connection pool sizing during incident | SSH inspection only | **`afterValueChanged` bind** | Kiponos for JVM pools |
| Per-tenant rate limits on API hot path | N/A | **Path-based limits** | Kiponos on 9k TPS filters |
| Cross-service saga timeout trees | No config semantics | **Nested ops tree** | Kiponos for choreography knobs |
| Java + Python aligned ops config | Not designed for this | **Both SDKs** | Kiponos for polyglot estates |
| Replacing bastion hosts and shared SSH keys | **Yes** | No | Boundary wins access hygiene |
| Replacing Vault for secrets | No — complementary | No — secrets stay in Vault | Three-way split: Vault / Boundary / Kiponos |
| Hot-path read latency | N/A — access plane | **SDK memory** | Kiponos on money path |

## When not to use Kiponos

| Use case | Better tool |
|----------|-------------|
| Secure SSH/RDP/DB session brokering with audit | **HashiCorp Boundary** |
| Revoking contractor access to production targets | **Boundary roles + OIDC** |
| Storing database passwords and API keys | **HashiCorp Vault** / cloud secrets manager |
| Infrastructure provisioning and IAM wiring | Terraform / cloud IAM |
| Interactive shell debugging and log tailing | **Boundary session** — Kiponos does not replace shell |

## When not to use Boundary (Kiponos is not the substitute)

| Use case | Better tool |
|----------|-------------|
| Brokering identity-based infrastructure access | **Boundary** — Kiponos is not an SSH bastion |
| Proving who SSH'd into which host for SOC2 | **Boundary session logs** |
| Replacing VPN for admin access to private subnets | **Boundary** + network policy |

## Getting started (15 minutes) — platform toolbox, not either/or

1. Keep Boundary for **SSH/RDP/DB session access** with OIDC and audit — unchanged.
2. [TeamPro at kiponos.io](https://kiponos.io) — profile `['payments']['platform']['prod']['live']`.
3. Mount Kiponos credentials via K8s Secret; add `sdk-boot-3` to payments Deployment and Python fraud worker.
4. Migrate **three keys** off static YAML: `resilience/partner/failure_rate_threshold`, `runtime/hikari/maximum_pool_size`, `fraud/block_score`.
5. Document runbook: *"Boundary for shell access to diagnose; Kiponos dashboard for threshold and pool changes across all replicas."*
6. Game day: SRE uses Boundary to inspect thread dump while platform lowers `failure_rate_threshold` in Kiponos — confirm all pods converge **without** `kubectl edit`.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [K8s Secrets vs live config boundary](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-secrets-vs-config-boundary.md)
- [Rate limits and circuit breakers](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-rate-limits-circuit-breakers.md)
- [Disaster recovery live config](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-disaster-recovery-live-config.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — Boundary for who reaches the host. Live hub for how every replica behaves once it is running.*