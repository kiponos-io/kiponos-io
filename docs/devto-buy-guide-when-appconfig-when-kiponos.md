---
title: "When AWS AppConfig, When Kiponos — Agent Poll vs WebSocket Ops Trees (Architecture)"
published: false
tags: architecture, aws, devops, java
description: AppConfig for AWS-native bootstrap; Kiponos for sub-second nested ops trees on saturated JVM/Python paths.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-buy-guide-when-appconfig-when-kiponos.md
main_image: https://files.catbox.moe/y1msbh.jpg
---

AWS architecture review. The proposal standardizes on **AWS AppConfig** for all runtime configuration — agent on every ECS task and EKS pod, SSM Parameter Store backend, CloudWatch alarms on deployment bake times. The latency SLO owner interrupts:

> "AppConfig agent polls every **45 seconds**. Our authorization path needs `failure_rate_threshold` to change in **seconds** when the processor degrades — not on the next poll cycle while we bleed error budget."

[AWS AppConfig](https://aws.amazon.com/systems-manager/features/appconfig/) is excellent for **AWS-native, gradually deployed application settings** — feature toggles, maintenance messages, bootstrap wiring validated through deployment strategies. [Kiponos.io](https://kiponos.io) is a **live operational config hub** — WebSocket deltas, nested ops trees, **local `get*()`** on saturated JVM and Python hot paths. This buyer guide helps platform leads decide during **latency SLO — move hot floats off poll path**.

## The problem — poll interval on the authorization hot path

Typical AppConfig agent pattern:

```java
// AppConfig agent writes to local file; app polls or watches file
@Scheduled(fixedDelay = 45_000)
public void refreshResilience() {
    String json = Files.readString(Path.of("/opt/aws/appconfig/output.json"));
    JsonNode node = mapper.readTree(json);
    this.failureRateThreshold = node.get("failure_rate_threshold").asInt(40);
}

public CircuitState evaluate(double rollingFailureRate) {
    if (rollingFailureRate > failureRateThreshold) {
        return CircuitState.OPEN;
    }
    return CircuitState.CLOSED;
}
```

AppConfig deployment strategy adds intentional lag:

```yaml
# AppConfig deployment — minutes for safe rollout
DeploymentStrategy:
  Name: Linear50PercentEvery30Seconds
  DeploymentDurationInMinutes: 30
```

During processor latency spike, you need `failure_rate_threshold` at **28** now. Poll-bound refresh means up to **45s staleness** — plus deployment bake if you pushed through AppConfig hosted configuration.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "AppConfig is real-time config" | Agent model is **poll/file** — not per-request local tree |
| "We will poll faster" | Faster poll **loads agents**; still not WebSocket delta |
| "Deployment strategies protect prod" | Strategies protect **rollouts** — wrong tool for **incident knobs** |
| "One AWS-native tool reduces vendors" | Hot-path floats need **different read contract** than bootstrap |
| "AppConfig + Parameter Store replaces a hub" | Flat parameters lack **nested fraud/resilience trees** |

## The Aha

**AppConfig deploys settings safely across AWS estates. Kiponos operates nested knobs on saturated request paths.** Use AppConfig for **bootstrap and gradual feature rollouts** tied to AWS deployment pipelines. Move **incident thresholds, pool sizes, and fraud floats** to Kiponos — WebSocket delta, local read, seconds to edit.

## What each tool owns

**AWS AppConfig:**

- Maintenance banners, default feature enablement per environment
- Configuration validated through Lambda validators before deploy
- Integration with CodePipeline, ECS, Lambda extensions
- Gradual rollout with bake time — **correct for product settings**

**Kiponos.io:**

- Profile `['payments']['prod']['live']` with nested `fraud/`, `resilience/`, `limits/`
- WebSocket snapshot + delta → SDK in-memory tree
- `kiponos.path("resilience", "payments").getFloat("failure_rate_threshold")` — local
- Dashboard edit lands in **seconds** — no deployment strategy wait

Bootstrap pattern — AppConfig points at Kiponos wiring:

```yaml
# AppConfig hosted config — bootstrap class only
kiponos:
  team_id: "team_abc"
  profile_path: "['payments']['prod']['live']"
feature_flags:
  maintenance_mode: false
```

Resilience floats **removed** from AppConfig JSON — live in hub.

## Architecture

![Architecture diagram](https://files.catbox.moe/ioucq3.png)

## Decision table — AppConfig vs Kiponos

| Key example | Tool | Why |
|-------------|------|-----|
| `maintenance_banner_text` | **AppConfig** | Gradual deploy; low read frequency |
| `new_onboarding_flow_default` | **AppConfig** | Product setting; bake time OK |
| `fraud.block_score` | **Kiponos** | Per-auth read; incident latency |
| `failure_rate_threshold` | **Kiponos** | Sub-second ops edit |
| `hikari.maximum_pool_size` | **Kiponos** | Binder + hot read |
| `kiponos.profile_path` | **AppConfig** | Bootstrap wiring |
| `replicaCount` | **GitOps / EKS** | Infra desired state |
| `saga.inventory.timeout_ms` | **Kiponos** | Cross-service ops tree |

## Boundary examples — Java integration

```java
@Configuration
public class HybridConfig {

    @Bean
    public Kiponos kiponos(
            @Value("${kiponos.team-id}") String teamId,
            @Value("${kiponos.access-key}") String accessKey,
            @Value("${kiponos.profile-path}") String profilePath) {
        // profile_path from AppConfig bootstrap — changes rarely
        return Kiponos.builder()
            .teamId(teamId)
            .accessKey(accessKey)
            .profilePath(profilePath)
            .build();
    }
}

@Service
public class PaymentCircuitOps {
    private final Kiponos kiponos;

    public boolean shouldOpen(double rollingFailureRate) {
        float threshold = kiponos.path("resilience", "payments")
            .getFloat("failure_rate_threshold");
        return rollingFailureRate > threshold;
    }

    public boolean maintenanceMode() {
        // Low-frequency read — OK from AppConfig-injected @Value refreshed on schedule
        return maintenanceModeFlag.get();
    }
}
```

### Python worker — same split

```python
import os
from kiponos import Kiponos

# Bootstrap from AppConfig-rendered env file at task start
os.environ["KIPONOS_PROFILE"] = os.environ.get(
    "KIPONOS_PROFILE", "['inference']['prod']['live']"
)
kiponos = Kiponos.create_for_current_team()

def worker_pool_size() -> int:
    return kiponos.path("ml", "embedding").get_int("worker_pool_size", 24)
```

## Config tree — operational keys off AppConfig

```yaml
fraud/
  thresholds/
    block_score: 79
    review_score: 65
resilience/
  payments/
    failure_rate_threshold: 28
    wait_duration_open_ms: 22000
  inventory/
    failure_rate_threshold: 42
limits/
  checkout/
    rpm: 9000
ml/
  embedding/
    worker_pool_size: 24
    batch_size: 32
```

Profile path: `['payments']['prod']['live']`.

## Real scenarios

| Scenario | AppConfig alone | Kiponos |
|----------|-----------------|---------|
| Processor latency — lower circuit threshold | Wait poll + deploy bake | Dashboard delta; next `getFloat()` |
| Enable maintenance banner | **AppConfig** linear deploy | N/A — correct tool |
| BIN attack — lower block score | 45s stale on auth path | Seconds |
| New env bootstrap profile path | **AppConfig** hosted config | Kiponos receives path at start |
| GPU OOM — shrink pool | Poll-bound | Live `worker_pool_size` |

## Performance — poll vs local read

- **AppConfig agent poll** — 15–60s typical; configuration change ≠ hot-path freshness
- **Deployment strategy** — intentional minutes — opposite of incident response
- **Kiponos WebSocket delta** — single key patch; next read immediate
- **Authorization path** — `getFloat()` is memory lookup; AppConfig refresh is file I/O + parse
- **Hybrid bootstrap** — AppConfig sets `profile_path` once; hub owns thousands of reads/sec

## Compare to alternatives

| Criterion | AWS AppConfig | Kiponos | Honest use |
|-----------|---------------|---------|------------|
| AWS-native gradual deploy | **Excellent** | N/A | AppConfig |
| Sub-second incident knob | Poor | **Excellent** | Kiponos |
| Nested ops trees | Flat JSON | **Native** | Kiponos |
| Hot-path local read | Poll/file lag | **SDK cache** | Kiponos |
| Lambda validation on publish | **Built-in** | Hub validators | AppConfig for bootstrap |
| Java + Python same tree | Custom | **Both SDKs** | Kiponos |
| Multi-cloud / on-prem | AWS-bound | **Hub** | Kiponos if portable |

## When not to use Kiponos

| Case | Use instead |
|------|-------------|
| ECS/EKS desired task count | Terraform / Cluster Autoscaler |
| AppConfig deployment strategy definitions | AWS console |
| IAM role ARNs | GitOps |
| Gradual product feature default rollout | **AppConfig** |
| Secrets | Secrets Manager / Vault |

## Getting started (15 minutes)

1. Inventory AppConfig hosted JSON — tag keys: **bootstrap** vs **operational**.
2. Move top five incident keys (`block_score`, one circuit threshold, one pool size) to Kiponos `['payments']['prod']['live']`.
3. Leave `kiponos.profile_path` and `maintenance_mode` in AppConfig.
4. Wire `PaymentCircuitOps` to Kiponos `getFloat()` — remove `@Scheduled` AppConfig poll for those keys.
5. Game day: simulate processor degradation; measure AppConfig poll path vs hub path to circuit open.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [When GitOps, when live config](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-buy-guide-when-gitops-when-live-config.md)
- [Kiponos vs Spring Cloud Config](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-vs-spring-cloud-config.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — AppConfig deploys settings on AWS time. The hub moves knobs on error-budget time.*