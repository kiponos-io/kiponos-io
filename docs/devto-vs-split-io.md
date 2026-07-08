---
title: "Kiponos vs Split.io — Feature Delivery & Experimentation vs Backend Ops Config Hub (Architecture)"
published: false
tags: architecture, devops, java, python
description: Split.io excels at traffic allocation, kill switches, and user-facing experiments. Kiponos excels at fraud thresholds, circuit breakers, and pool sizes with zero-latency reads on backend hot paths. Honest comparison — complementary, not competitive.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-vs-split-io.md
main_image: https://files.catbox.moe/x854ey.jpg
---

Thursday 14:22. The marketplace team runs a **Split treatment** on `new_seller_dashboard` — 12% traffic allocation, impression events flowing to the warehouse, kill switch ready if conversion dips. In the same incident bridge, the ledger service pages: a partner API is timing out and the SRE needs `ledger/partner_timeout_ms` dropped from 8000 to 3500, `fraud/velocity_cap` raised to 22, and the Python reconciliation worker needs `batch_commit_size` cut from 500 to 120 before the queue backs up into Kafka.

The product manager says:

> "Split has **dynamic configuration** and **traffic allocation** — put the partner timeout in a config split. One SDK for experiments and ops."

The backend lead on the ledger path answers:

> "Split is built for **who gets the new dashboard** and **whether we kill a feature**. Our ledger workers evaluate **numeric thresholds 11k times per second** during a partner outage — not user cohorts with impression telemetry."

[Split.io](https://www.split.io) is a mature **feature delivery and experimentation platform** — traffic allocation, kill switches, impression tracking, and statistical experiment analysis tied to user identity. [Kiponos.io](https://kiponos.io) is a **live operational config hub** — nested trees, WebSocket deltas, and local `get*()` reads in Java Spring Boot 3 and Python on backend hot paths. Strong organizations use Split where product learns; Kiponos where ledger, fraud, and reconciliation survive.

## The problem — experiment infrastructure on the ledger hot path

Typical Split integration for a product treatment:

```java
// Product path — correct Split usage
SplitClient split = splitFactory.client();
String treatment = split.getTreatment(userKey, "new_seller_dashboard");
if ("on".equals(treatment)) {
    return renderV2Dashboard(seller);
}
return renderLegacyDashboard(seller);
```

Teams then route backend knobs through dynamic config:

```java
// Anti-pattern — ops float through experiment infrastructure
Map<String, Object> attrs = Map.of("partner_id", partnerId);
String configTreatment = split.getTreatmentWithConfig("system", "ledger_tuning", attrs)
        .config();
int timeoutMs = (int) configTreatment.getOrDefault("partner_timeout_ms", 8000);

if (elapsedMs > timeoutMs) {
    return LedgerDecision.timeout();
}
```

Friction shows up fast:

- **Identity-centric evaluation** — partner timeouts are **system-bound**, not seller-bound; synthetic `system` keys are a workaround
- **Traffic allocation semantics** — designed for **gradual feature exposure**, not sub-second incident tuning of one integer
- **Impression and telemetry pipeline** — valuable for experiments; overhead you do not want on **ledger authorization at 11k TPS**
- **Kill switches** — excellent for turning off `new_seller_dashboard`; awkward for `fraud/velocity_cap` nested beside ML batch sizes in a flat split namespace
- **Python reconciliation workers and Java ledger APIs** — duplicate dynamic config or custom sync scripts

Split dynamic config is legitimate for **product-tunable parameters** (default sort order, onboarding step count). It is the wrong primitive for **incident knobs** on saturated backend paths.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Split dynamic config replaces an ops config hub" | Built for **treatment parameters**, not nested platform trees |
| "Kill switches cover every runtime toggle" | Kill switches target **user-facing features**, not circuit thresholds |
| "Traffic allocation is how we roll out everything" | Ops knobs need **immediate global effect**, not 12% cohort exposure |
| "One Split SDK simplifies the estate" | Product experiments and fraud floats have **different latency budgets** |
| "Impression events are negligible" | At 11k ledger evaluations/sec, telemetry adds **cost and complexity** on the hot path |

## The Aha

**Split.io decides which users see which features, how traffic is allocated, and when to kill a rollout. Kiponos decides how hard backend systems run when partners degrade and fraud spikes.** Keep `new_seller_dashboard` in Split with traffic allocation and kill-switch semantics. Move `partner_timeout_ms`, `velocity_cap`, and `batch_commit_size` to Kiponos — local reads, no per-transaction treatment evaluation.

## What Kiponos.io is for Split-heavy product orgs

Kiponos is a real-time configuration hub. Java and Python SDKs connect once via WebSocket, hydrate a typed profile tree, and serve `getInt()`, `getDouble()`, and `getBoolean()` from **in-process memory**. Dashboard edits push **single-key deltas** — change `fraud/thresholds/velocity_cap` from 15 to 22; every pod and worker sees it without redeploy or traffic reallocation.

Profile path for this comparison:

```
['marketplace']['ledger']['prod']['live']
```

User-facing feature treatments stay in Split. Backend operational knobs live in Kiponos — the **read contract** is always local cache lookup, not `getTreatmentWithConfig()` with attribute maps.

## Architecture — Split feature delivery vs Kiponos ops plane

![Architecture diagram](https://files.catbox.moe/f1gzlm.png)

Hybrid is the norm: Split owns **identity-bound** feature delivery and experiments; Kiponos owns **system-bound** thresholds both runtimes read.

## Config tree — backend ops keys that do not belong in Split treatments

```yaml
fraud/
  thresholds/
    velocity_cap: 22
    block_score: 81
    review_score: 67
    bin_attack_mode: true
ledger/
  partner/
    timeout_ms: 3500
    retry_max: 2
    circuit_failure_rate: 38
  reconciliation/
    batch_commit_size: 120
    max_inflight_batches: 8
    stale_entry_hours: 72
resilience/
  payments/
    failure_rate_threshold: 32
    wait_duration_open_ms: 24000
    half_open_permitted_calls: 6
ml/
  scoring/
    model_version: v4.2
    inference_timeout_ms: 180
    fallback_score: 50
split_bridge/
  # Document which treatments remain on Split
  new_seller_dashboard: split_owned
  checkout_redesign_kill_switch: split_owned
```

## Java integration — ledger path stays local

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
public class PartnerLedgerGate {

    private final Kiponos kiponos;

    public PartnerLedgerGate(Kiponos kiponos) {
        this.kiponos = kiponos;
    }

    public TimeoutDecision evaluatePartnerCall(String partnerId, long elapsedMs) {
        var ledger = kiponos.path("ledger", "partner");
        int timeoutMs = ledger.getInt("timeout_ms");
        int maxRetries = ledger.getInt("retry_max");

        if (elapsedMs > timeoutMs) {
            return TimeoutDecision.abort(partnerId, maxRetries);
        }
        return TimeoutDecision.continueCall();
    }

    public boolean velocityExceeded(int eventsLastHour) {
        int cap = kiponos.path("fraud", "thresholds").getInt("velocity_cap");
        return eventsLastHour > cap;
    }
}
```

Product treatment — keep Split on the seller dashboard where traffic allocation and kill switches matter:

```java
public SellerDashboard routeDashboard(Seller seller) {
    String treatment = splitClient.getTreatment(seller.getId(), "new_seller_dashboard");
    if ("on".equals(treatment)) {
        return renderV2(seller);
    }
    if ("killed".equals(treatment)) {
        return renderLegacyWithBanner(seller, "dashboard_rolled_back");
    }
    return renderLegacy(seller);
    // Do not route ledger.partner_timeout_ms through Split
}
```

## Python integration — reconciliation worker reads same ops tree

```python
import os
from kiponos import Kiponos

os.environ["KIPONOS_PROFILE"] = "['marketplace']['ledger']['prod']['live']"
kiponos = Kiponos.create_for_current_team()

def batch_commit_size() -> int:
    return kiponos.path("ledger", "reconciliation").get_int("batch_commit_size", 500)

def on_config_change(change):
    if change.path.startswith("ledger/reconciliation/batch_commit_size"):
        resize_commit_buffer(int(change.new_value))

kiponos.after_value_changed(on_config_change)
```

Split has no first-class story for a **Python Kafka consumer** and a **Java ledger cluster** sharing `ledger/reconciliation/batch_commit_size` with sub-second incident edits outside any traffic allocation window.

## Real scenarios

| Event | Split alone | Split + Kiponos |
|-------|-------------|-----------------|
| Roll out `new_seller_dashboard` at 12% | **Native traffic allocation** | Keep Split; unchanged |
| Kill switch on bad checkout UI treatment | **Native kill switch** | Keep Split; unchanged |
| Partner API brownout — shorten timeout | Dynamic config + synthetic user key | `ledger/partner/timeout_ms` live |
| Fraud velocity spike during ledger surge | Wrong tool / awkward config split | `fraud/thresholds/velocity_cap` in seconds |
| Kafka backlog — shrink commit batches | Not the tool | `ledger/reconciliation/batch_commit_size` in Python |
| Measure experiment lift on seller dashboard | **Split impressions + stats** | Keep Split; ops keys in Kiponos audit log |
| Cross-service circuit alignment | Flat config namespace | Shared nested `resilience/` tree |

## Performance — ledger hot path economics

- **Split treatment evaluation** — attribute maps, bucketing, impression pipeline — right for **product paths at human interaction scale**
- **Split dynamic config on ledger** — per-evaluation config semantics; not designed for **11k bare integers/sec** on partner timeout checks
- **Kiponos `getInt()`** — in-memory tree lookup; zero network on the authorization and ledger path
- **Delta updates** — incident changes one key; no treatment redeploy or traffic reallocation wait
- **Kill switch vs ops knob** — Split kills `new_seller_dashboard` for users; Kiponos tunes `failure_rate_threshold` for systems — different control planes, different SLAs
- **One WebSocket per JVM/worker** — background sync; hot path never blocks on vendor RTT

## Honest comparison table

| Criterion | Split.io | Kiponos | Honest verdict |
|-----------|----------|---------|----------------|
| Traffic allocation & gradual rollouts | **Excellent** | App-side bucketing possible | Split wins feature delivery |
| Kill switches for user-facing features | **Core strength** | Global boolean keys possible | Split for product rollback |
| Experiment stats & impression tracking | **Built-in** | Ops change log only | Split for measured experiments |
| Dynamic config for product parameters | **Good** | Good for ops trees | Split for UX tuning tied to treatments |
| Backend incident knobs (fraud, timeouts) | Awkward fit | **First-class** | Kiponos on ledger path |
| Nested cross-service ops trees | Flat split namespaces | **Hierarchical paths** | Kiponos for platform ops |
| Hot-path read at 11k TPS | Treatment evaluation model | **Local cache** | Kiponos on money path |
| Java + Python same hub | Partial / role-dependent | **Both SDKs** | Kiponos for polyglot backend |
| User identity targeting rules | **Rich** | Profile paths + app logic | Split for cohort experiments |
| Pricing model | Event / seat oriented | Team/hub pricing | Model your experiment vs ops split |

## When not to use Kiponos

| Use case | Better tool |
|----------|-------------|
| Traffic allocation with statistical experiment analysis | **Split.io** |
| Kill switch on a user-facing feature rollout | **Split.io** |
| Impression tracking tied to treatment exposure | **Split.io** |
| Bootstrap secrets and API keys | Vault / cloud secret manager |
| Infrastructure desired state | GitOps / Terraform |

## Getting started (15 minutes) — split feature delivery from backend ops

1. Inventory every live key: mark **product treatment** (traffic allocation, kill switch, experiment) vs **operational knob** (fraud, ledger timeout, batch size).
2. [TeamPro at kiponos.io](https://kiponos.io) — profile `['marketplace']['ledger']['prod']['live']`.
3. Migrate **three ops keys** off Split dynamic config: `partner_timeout_ms`, one `velocity_cap`, one `batch_commit_size`.
4. Wire Java `PartnerLedgerGate` and Python reconciliation worker to the same profile.
5. Document RFC: *"Split owns traffic allocation, kill switches, and user-facing experiments; Kiponos owns backend ops floats on hot paths."*

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [Feature flags vs config hub (architecture)](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-feature-flags-vs-config-hub.md)
- [Kiponos vs LaunchDarkly](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-vs-launchdarkly-feature-flags.md)
- [Kiponos vs Statsig](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-vs-statsig.md)
- [Fraud payment routing](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-fraud-payment-routing.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — Split for which users get the treatment. Live hub for how hard the ledger runs during the outage.*