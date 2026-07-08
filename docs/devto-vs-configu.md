---
title: "Kiponos vs Configu — ConfigOps Orchestration vs Live Hot-Path Ops Trees (Architecture)"
published: false
tags: configops, devops, java, python
description: Configu excels at .cfgu schemas, environment promotion, and CI-driven ConfigOps across stores. It was never built for sub-second float reads on Java payment filters. Honest comparison with Spring Boot 3 and Python patterns.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-vs-configu.md
main_image: https://files.catbox.moe/x854ey.jpg
---

Thursday 09:47. Platform engineering adopted **[Configu](https://configu.com)** for ConfigOps: `.cfgu` schemas in Git, orchestrator promoting `staging → production`, CLI validating keys before merge. The fraud squad applauds — until authorization holds at 9k TPS during a BIN attack and someone tries to lower `fraud/block_score` through the promotion pipeline.

The ConfigOps lead explains the model:

> "Configu orchestrates **desired state across stores** — SSM, Vault, dotenv files — with schema validation in CI. It is not a **sub-second in-process read layer** inside your Spring Boot filter."

Configu is excellent for **governed configuration supply chains** — who may change what, which environment gets which snapshot, audit in Git. [Kiponos.io](https://kiponos.io) is excellent for **operational runtime knobs** that Java and Python read locally on every request via WebSocket deltas. Complementary layers in a mature platform.

## The problem — orchestration latency on the authorization hot path

Typical Configu workflow:

```bash
# CI promotion — schema-validated export to target store
configu upsert --store aws-ssm --set payments/prod/fraud --from ./config/fraud.cfgu
configu eval payments/prod/fraud --schema ./schemas/fraud.cfgu
```

`.cfgu` schema enforces types and constraints — genuine strength:

```cfgu
fraud.block_score:
  type: Number
  required: true
  schema:
    minimum: 0
    maximum: 100
```

Application code still needs a **read model** at runtime:

```java
// After Configu wrote to SSM — app still polls or caches Parameter Store
int blockScore = ssmClient.getParameter("payments/prod/fraud/block_score")
        .parameter().value(); // network or stale cache
```

Teams report friction when ConfigOps meets incidents:

- **Promotion pipeline** — staging → prod is correct for governance; wrong for **3 AM integer tweak**
- **Store is source of truth** — SSM, Vault, files — not **in-process tree** on hot path
- **Schema validation in CI** — excellent gate; does not push **delta to running JVM** in seconds
- **Python workers + Java APIs** — same keys, but each runtime implements **its own poll/cache** unless you add another layer

Configu is not wrong. **Sub-second operational reads inside payment filters** are outside its orchestration sweet spot.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Configu gives us live config" | It gives **governed desired state** — apps still pull from stores |
| "One orchestrator replaces all config tools" | Orchestration ≠ **zero-latency SDK read contract** |
| ".cfgu schema means runtime safety" | Schema validates **before deploy** — not per-request in JVM |
| "SSM after Configu upsert is real-time" | Parameter Store read is **network** — cache TTL applies |
| "We will poll every 5 seconds" | 5s staleness at 9k TPS is **millions of wrong decisions** during BIN attack |

## The Aha

**Configu owns ConfigOps — schemas, stores, environment promotion, CI gates. Kiponos owns operational knobs that running processes read locally without store round-trips.** Keep Configu for bootstrapping secrets into Vault and promoting reviewed YAML into SSM. Move incident knobs — fraud thresholds, tenant RPM, circuit floats — into a live hub the SDK holds in memory.

## What Kiponos.io is in a ConfigOps estate

Kiponos is a real-time configuration hub. SDK connects via WebSocket, loads profile `['payments']['platform']['prod']['live']`, serves `getInt()` / `getBool()` from **in-process memory**. Dashboard edit → delta → next authorization sees new `block_score` — **no Configu upsert, no SSM poll, no redeploy**.

Configu can remain the **governance layer** for bootstrap values written once per release:

```
Configu CI  →  Vault / SSM (bootstrap)     Kiponos hub  →  runtime ops tree
```

Profile path for live ops:

```
['payments']['platform']['prod']['live']
```

Everything under `payments_ops/` is hub-native. JDBC URLs and API keys still flow through Configu → store → Spring `@Value` at startup.

## Architecture — ConfigOps orchestration vs Kiponos runtime hub

![Architecture diagram](https://files.catbox.moe/2yyhxv.png)

## Config tree — runtime ops alongside Configu-managed bootstrap

```yaml
payments_ops/
  fraud/
    thresholds/
      block_score: 88
      review_score: 72
      velocity_per_hour: 18
  resilience/
    partner/
      failure_rate_threshold: 35
      wait_duration_open_ms: 20000
      half_open_calls: 6
  limits/
    default/
      rpm: 1500
    tenant_acme/
      rpm: 7500
      burst: 1000
  configu_bridge/
    # Document keys still promoted via Configu → SSM at release cadence
    datasource_pool_max: 40
    feature_new_ledger_enabled: false
```

## Java integration — Spring Boot 3, hot-path read + controlled binds

```java
@Configuration
public class KiponosConfig {

    @Bean
    public Kiponos kiponos(
            @Value("${kiponos.team-id}") String teamId,
            @Value("${kiponos.access-key}") String accessKey,
            @Value("${kiponos.profile-path}") String profilePath) {
        Kiponos client = Kiponos.builder()
                .teamId(teamId)
                .accessKey(accessKey)
                .profilePath(profilePath)
                .build();
        client.afterValueChanged(change -> {
            if (change.path().endsWith("failure_rate_threshold")) {
                log.info("Partner circuit threshold now {}", change.newValue());
            }
        });
        return client;
    }
}
```

```java
@Service
public class AuthorizationService {

    private final Kiponos kiponos;

    public AuthorizationService(Kiponos kiponos) {
        this.kiponos = kiponos;
    }

    public Decision authorize(PaymentRequest request) {
        int blockScore = kiponos.path("payments_ops", "fraud", "thresholds")
                .getInt("block_score", 80);
        int reviewScore = kiponos.path("payments_ops", "fraud", "thresholds")
                .getInt("review_score", 65);
        int score = fraudEngine.score(request);
        if (score >= blockScore) {
            return Decision.block("score_exceeded");
        }
        if (score >= reviewScore) {
            return Decision.review("elevated_risk");
        }
        return Decision.approve();
    }
}
```

`getInt()` on the hot path — no SSM `GetParameter` per authorization.

## Python integration — Celery-style fraud worker

```python
import os
import logging
from kiponos import Kiponos

log = logging.getLogger(__name__)

os.environ["KIPONOS_ID"] = os.environ["KIPONOS_ID"]
os.environ["KIPONOS_ACCESS"] = os.environ["KIPONOS_ACCESS"]
os.environ["KIPONOS_PROFILE"] = "['payments']['platform']['prod']['live']"

kiponos = Kiponos.create_for_current_team()

def on_threshold_change(change):
    if "block_score" in change.path:
        log.info("Fraud block_score updated to %s", change.new_value)

kiponos.after_value_changed(on_threshold_change)

def should_block_card(txn_count_last_hour: int, risk_score: int) -> bool:
    block_score = kiponos.path("payments_ops", "fraud", "thresholds").get_int("block_score", 80)
    velocity = kiponos.path("payments_ops", "fraud", "thresholds").get_int("velocity_per_hour", 15)
    return risk_score >= block_score or txn_count_last_hour > velocity
```

Same profile and tree as Java — Configu does not need a second store export for every ops tweak.

## Real scenarios

| Event | Configu alone | Configu + Kiponos |
|-------|---------------|-------------------|
| BIN attack — raise block score | CI promote → SSM upsert → app cache TTL | `fraud/thresholds/block_score` in seconds |
| New tenant — raise RPM limit | Schema PR + env promotion | `limits/tenant_acme/rpm` live |
| Partner brownout — circuit threshold | Store write + poll delay | `resilience/partner/failure_rate_threshold` immediate |
| Audit new secret into Vault | **Native strength** — governed upsert | Keep Configu for this path |
| Validate config shape before prod | **.cfgu schema in CI** | Keep Configu; hub keys documented in bridge folder |
| Java + Python same fraud knobs | Two store readers + cache logic | One tree, two SDKs |

## Performance — authorization path specifics

- **Configu upsert to SSM** — correct for **release cadence**; not incident-second latency to JVM
- **SSM GetParameter on hot path** — network hop or **stale local cache** at 9k TPS
- **Kiponos read** — in-process tree lookup every `authorize()` call
- **Delta propagation** — single `block_score` change is bytes via WebSocket, not full store document
- **Configu CI eval** — runs at merge time; Kiponos dashboard edit runs **during active incident**

## Honest comparison table

| Criterion | Configu | Kiponos | Honest verdict |
|-----------|---------|---------|----------------|
| .cfgu schema validation in CI | **Excellent** | Hub typing via conventions | Configu for governance gates |
| Multi-store orchestration (SSM, Vault, files) | **Native** | Single hub model | Configu for supply chain |
| Environment promotion staging → prod | **Core strength** | Profile paths per env | Configu for reviewed promotion |
| Sub-second hot-path float read | Store-dependent | **SDK local memory** | Kiponos on 9k TPS filters |
| WebSocket delta to running process | Not the model | **Core model** | Kiponos for live ops |
| Structured ops trees across services | Store key conventions | **Path-based tree** | Kiponos for nested `fraud/thresholds/` |
| Java + Python unified runtime reads | Per-runtime store SDK | **Both SDKs** | Kiponos for polyglot hot path |
| Open-source ConfigOps CLI | **configu/cli on GitHub** | Commercial hub + OSS SDKs | Different layers |

## When not to use Kiponos

| Use case | Better tool |
|----------|-------------|
| Schema-validated promotion across SSM, Vault, dotenv | **Configu** |
| Git-reviewed bootstrap secrets per environment | **Configu + target store** |
| Infrastructure-as-code desired state only | **Terraform / Pulumi** |
| One-time wiring keys that never change in production | Config Server / env files |
| Compliance export of all config to immutable audit store | **Configu orchestrator** |

## Getting started (15 minutes) with Configu still in place

1. Keep Configu for **.cfgu schemas**, CI validation, and **bootstrap promotion** to Vault/SSM — unchanged.
2. [TeamPro at kiponos.io](https://kiponos.io) — profile `['payments']['platform']['prod']['live']`.
3. Add `sdk-boot-3` to payments service; Python worker uses same `KIPONOS_PROFILE`.
4. Migrate **three runtime keys**: `fraud/thresholds/block_score`, partner circuit threshold, one tenant RPM.
5. Document `configu_bridge/` keys in the hub tree so SREs know what stays in ConfigOps vs live hub.
6. Run game day: dashboard tweak vs Configu promote → SSM → cache expiry timer.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [Spring Cloud Config comparison](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-vs-spring-cloud-config.md)
- [Rate limits live](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-rate-limits-circuit-breakers.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — Configu for governed ConfigOps supply chains. Live hub for runtime knobs that cannot wait for store promotion.*