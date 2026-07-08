---
title: "SLO Burn Multiplier Live — Tune Alert Sensitivity Before Error Budget Is Gone (Java SDK)"
published: false
tags: java, sre, observability, devops
description: Burn-rate constants in alert YAML need a merge to change during incidents. Kiponos holds multipliers ops adjusts while pagers are hot.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-reliability-slo-burn-multiplier-live.md
main_image: https://files.catbox.moe/87bzmo.jpg
---

Tuesday 09:41 UTC. A fiber cut in `eu-west-2` ripples through three edge POPs. Latency on the platform gateway climbs for eleven minutes — not a full outage, but enough to trip **multi-window burn-rate alerts** on `checkout-availability` and `api-latency-p99`. PagerDuty fires six pages in four minutes. The bridge channel fills with "known blip, ignore" messages while the error budget graph bleeds red.

The SRE lead opens `slo-alerts.yaml` and finds the knob everyone needs right now:

```yaml
burnRateMultiplier: 2.0
```

Raising `burn_multiplier` would dampen short-window sensitivity during a confirmed multi-region blip — but that constant lives in Git beside the Prometheus rules. Changing it means a PR, a pipeline, and recycling the alert-evaluator pods while the bridge is still hot.

> "Burn sensitivity is **operational**. Why are we negotiating with CI while the budget clock runs?"

[Kiponos.io](https://kiponos.io) treats `burn_multiplier` like any other hot-path float: WebSocket deltas into an in-memory SDK tree, local `getDouble()` on every evaluation tick, no redeploy.

## The problem — burn_multiplier baked into static config

Your burn-rate evaluator runs every thirty seconds inside the platform observability service:

```java
@Service
public class SloBurnEvaluator {

    private static final double BURN_RATE_MULTIPLIER = 2.0;

    public boolean shouldPage(String sloName, double shortWindowBurn, double longWindowBurn) {
        double threshold = sloTarget(sloName).errorBudgetBurnRate() * BURN_RATE_MULTIPLIER;
        return shortWindowBurn > threshold && longWindowBurn > threshold * 0.5;
    }
}
```

The multiplier also appears in Spring YAML beside Resilience4j unrelated keys:

```yaml
platform:
  slo:
    burn_multiplier: 2.0
    short_window_minutes: 5
    long_window_minutes: 60
```

During a multi-region blip, you need to raise `burn_multiplier` to `4.0` **now** — not after review merges and evaluator pods restart. Static config means either **alert fatigue** (pages keep firing on a known transient) or **manual silences** that hide the next real regression.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Burn rules are code — they belong in Git" | Sensitivity is an **ops dial** during blips, not semver |
| "Silence rules in PagerDuty for known issues" | Silences expire; budgets still drain; handoff is messy |
| "Multi-window alerts are self-tuning" | Windows are fixed; multiplier is the human judgment layer |
| "We will tune after the postmortem" | Error budget is gone before the doc ships |
| "Observability config is not on the hot path" | Evaluators run continuously — reads add up at fleet scale |

## The Aha

**burn_multiplier is operational config** — it changes during incidents, regional blips, and launch windows. It belongs in a **live tree** the evaluator already reads with `getDouble()`, not in a `static final` imported at boot.

## What Kiponos.io is for SLO burn tuning

Connect once at startup with profile `['platform']['prod']['slo']`. The hub hydrates `burn_multiplier`, window lengths, and per-SLO overrides into every SDK instance. Dashboard edits send **deltas** — one float patch, not a full alert bundle redeploy.

On the evaluation loop, `kiponos.path("slo", "burn").getDouble("burn_multiplier")` is a **local memory read**. Zero HTTP. No Redis poll between Prometheus scrapes.

`afterValueChanged` lets you log audit trails and reset in-memory rolling windows when ops flips `blip_mode` — without recycling the JVM.

Honest boundary: Kiponos does **not** replace Prometheus rule storage, Terraform for alert routing, or PagerDuty schedule design. It owns **operational floats** your Java evaluator reads thousands of times per hour.

## Architecture

![Architecture diagram](https://files.catbox.moe/0to7qj.png)

## Config tree

```yaml
slo/
  burn/
    burn_multiplier: 2.0
    short_window_minutes: 5
    long_window_minutes: 60
    enabled: true
  blip/
    blip_mode: false
    blip_multiplier: 4.0
    auto_expire_minutes: 30
  targets/
    checkout_availability/
      objective: 0.999
      burn_multiplier_override: 0
    api_latency_p99/
      objective: 0.995
      burn_multiplier_override: 0
  ops/
    owner: platform-sre
    notes: "Raise multiplier during confirmed regional blips only"
```

## Integration (Spring Boot 3)

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
public class SloBurnEvaluator {

    private final Kiponos kiponos;
    private final BurnWindowTracker windowTracker;

    public SloBurnEvaluator(Kiponos kiponos, BurnWindowTracker windowTracker) {
        this.kiponos = kiponos;
        this.windowTracker = windowTracker;
        kiponos.afterValueChanged(this::onSloConfigChange);
    }

    public boolean shouldPage(String sloName, double shortWindowBurn, double longWindowBurn) {
        double multiplier = resolveMultiplier(sloName);
        double threshold = sloTarget(sloName).errorBudgetBurnRate() * multiplier;
        return shortWindowBurn > threshold && longWindowBurn > threshold * 0.5;
    }

    private double resolveMultiplier(String sloName) {
        var override = kiponos.path("slo", "targets", sloName)
                .getDouble("burn_multiplier_override", 0);
        if (override > 0) {
            return override;
        }
        if (kiponos.path("slo", "blip").getBool("blip_mode", false)) {
            return kiponos.path("slo", "blip").getDouble("blip_multiplier", 4.0);
        }
        return kiponos.path("slo", "burn").getDouble("burn_multiplier", 2.0);
    }

    private void onSloConfigChange(ValueChange change) {
        if (change.path().startsWith("slo/blip") || change.path().startsWith("slo/burn")) {
            windowTracker.resetShortWindow();
            log.warn("SLO burn policy changed: {} → {} — short window reset",
                    change.path(), change.newValue());
        }
    }
}
```

Ops enables `blip_mode` and sets `blip_multiplier: 4.0` during the fiber cut. The next evaluation tick uses the higher multiplier — pages stop for the known blip, budget drain visibility stays honest on the long window.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| Multi-region blip — raise multiplier temporarily | PR + evaluator pod rollout | Enable `blip_mode` in dashboard; next tick dampens |
| Launch window — noisy canary | Manual PagerDuty silences | Per-SLO `burn_multiplier_override` live |
| Post-blip recovery | Second deploy to restore `2.0` | Disable `blip_mode`; audit trail in hub |
| FinOps review — too many pages last quarter | Debate YAML in architecture forum | Export hub change log with actor timestamps |
| On-call handoff | "We silenced checkout" in Slack | `ops/notes` + live multiplier visible in tree |

## Performance on the burn evaluation path

- **`getDouble()` local read** — microseconds per eval tick vs Prometheus query RTT
- **Delta patch** — changing `burn_multiplier` from 2.0 → 4.0 sends one float, not full rules YAML
- **`afterValueChanged` on blip flip** — window reset runs once per policy change, not per scrape
- **One WebSocket per evaluator JVM** — not one config HTTP fetch per thirty-second loop
- **Per-SLO overrides** — nested folders without separate microservices per objective

## Compare to alternatives

| Approach | Tune multiplier during blip | Hot-path read cost | Nested SLO trees |
|----------|----------------------------|-------------------|------------------|
| YAML + redeploy | 20+ minutes | N/A until restart | Awkward |
| PagerDuty silences only | Fast but opaque | N/A | No budget coupling |
| Spring Cloud Config + refresh | Context recycle | Network on refresh | Medium |
| Feature-flag SaaS | Good for booleans | Network RTT | Poor for per-SLO floats |
| **Kiponos live hub** | **Seconds** | **Local get*()** | **First-class** |

## When not to use Kiponos

| Case | Use instead |
|------|-------------|
| Prometheus recording rule expressions | Git-reviewed rule files |
| PagerDuty schedule and escalation policies | Terraform / PD admin |
| SLO objective definitions (99.9% vs 99.95%) | Architecture review + Git |
| Long-term error budget policy documents | SRE handbook + compliance wiki |
| One-time bootstrap defaults for new services | Git-reviewed YAML is fine |

## Getting started (15 minutes)

1. Sign up at [kiponos.io](https://kiponos.io) (TeamPro).
2. Create profile path `['platform']['prod']['slo']`.
3. Add `io.kiponos:sdk-boot-3` to your alert-evaluator or platform observability service.
4. Set `KIPONOS_ID`, `KIPONOS_ACCESS`, and `-Dkiponos="['platform']['prod']['slo']"`.
5. Move `burn_multiplier` out of YAML into the hub tree under `slo/burn/`.
6. Wire `SloBurnEvaluator` with `afterValueChanged` for blip mode audit logging.
7. Staging game day: inject synthetic burn spike, enable `blip_mode`, confirm pages dampen **without pod restart**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [Rate limits and circuit breakers](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-rate-limits-circuit-breakers.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*burn_multiplier belongs in the live ops tree — not in constants that drain your error budget while CI runs.*