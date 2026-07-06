---
title: "Error Budget Burn Alerts as Live Thresholds — Pause Releases Before SLO Debt Compounds (Java SDK)"
published: false
tags: java, sre, architecture, observability
description: Burn-rate alerts trapped in Prometheus YAML cannot gate deploys mid-incident. Kiponos holds burn thresholds, release pause flags, and escalation tiers in one live tree — SRE tightens policy while JVMs keep running.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-error-budget-burn-alert.md
main_image: https://files.catbox.moe/87bzmo.jpg
---

Monday 09:22 UTC. **checkout-api** error budget for the monthly availability SLO is **62% consumed** with eighteen days left in the window. PagerDuty fires a 2-hour burn alert — good. But the deploy pipeline still promotes **payments-v2** canary from 25% → 50% because the release gate reads a **static** `MAX_BURN_RATE=3.0` baked into `ReleaseGate.java` since last quarter's tuning exercise.

Forty minutes later, a regression in v2 pushes burn to **4.8×**. The SRE on-call edits the Grafana annotation. Nobody edits the gate. Canary weight stays at 50% until someone remembers step 11 in the incident doc: *"open PR to lower `MAX_BURN_RATE` in Helm."*

The engineering manager asks the question every SLO review eventually surfaces:

> "Why does our **alert** know we're burning budget faster than our **release system** can react?"

Most Java platform teams split error-budget policy across **three artifacts**: Prometheus alert rules, a wiki runbook for "pause releases," and a hard-coded comparator in the deploy router. [Kiponos.io](https://kiponos.io) collapses that into **one operational tree** — burn thresholds, pause flags, and per-service escalation — readable on every release decision with **local `get*()` calls** and adjustable from the dashboard while processes run.

## The problem: burn alerts that do not drive behavior

A typical release gate checks budget like this:

```java
@PostMapping("/deploy/promote")
public PromoteResponse promote(@RequestBody PromoteRequest req) {
    double burnRate = sloClient.currentBurnRate("checkout-api");
    if (burnRate > MAX_BURN_RATE) {  // static final 3.0
        throw new ReleaseBlockedException("burn too high");
    }
    return deployService.promoteCanary(req.getTargetWeight());
}
```

Burn policy usually lives elsewhere — scattered and stale:

```yaml
# application-prod.yml — requires rolling restart to change
slo:
  max-burn-rate: 3.0
  pause-all-releases: false
  fast-burn-window-minutes: 120
```

Or worse — only in Alertmanager, with no connection to the deploy path:

```yaml
# alerts/checkout-slo.yml — fires pages, does not gate code
- alert: CheckoutFastBurn
  expr: slo:burn_rate_2h > 3
  for: 5m
```

The release router executes **dozens of promote calls per day**. During a burn spike you need to:

1. Lower **`burn.max_rate_2h`** before the next canary step lands
2. Flip **`releases.pause_canary`** so automated pipelines stop without a merge queue
3. Raise **`escalation.notify_director`** when budget remaining drops below a live floor

Doing that through Helm while pages are firing is not SRE — it is **alert theater**.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Burn alerts are enough — humans will pause deploys" | Humans are in war rooms; bots keep promoting |
| "We'll wire Alertmanager to Argo CD" | Webhook lag and brittle YAML forks per service |
| "3.0 burn rate is industry standard" | Checkout and search have different budget shapes |
| "Freeze deploys is a boolean in Slack" | Slack pins scroll away; pipeline has no reader |
| "Monthly SLO review updates thresholds" | Prod gate still shows Q1 numbers in July |

## The architecture insight

**Error budget policy is operational config, not monitoring archaeology.** The same knobs your burn runbook tells humans to edit — max burn rate, pause switches, escalation tiers — belong in **one live tree** the JVM already reads on the promote path. Kiponos makes "pause canary" and "tighten burn ceiling" a **dashboard edit**, not a deploy pipeline.

## What Kiponos.io is for error budget gates

[Kiponos.io](https://kiponos.io) is a real-time configuration hub. Each Spring Boot platform service connects **once** at startup over WebSocket; the profile tree — for example `['platform']['sre']['prod']['live']` — loads into an **in-memory cache** inside the Java SDK.

When SRE sets `burn.max_rate_2h` to `2.0` and `releases.pause_canary` to `true`, a **delta** patches only those keys. The next `kiponos.path("burn", "checkout").getFloat("max_rate_2h")` on an incoming promote request is a **local memory read** — no HTTP to a config API, no JDBC poll, no Redis round-trip on the release path.

`afterValueChanged` listeners let you log audit trails, increment metrics, and notify PagerDuty when pause flips **without** restarting the JVM.

No restart. No redeploy. No `@RefreshScope` bean recycle.

## Reference architecture

![Architecture diagram](https://files.catbox.moe/t04vad.png)

**Alerts inform humans; the tree gates machines.** Keep burn-rate graphs in Grafana for context — but the **authoritative thresholds** live in Kiponos where tightening them takes seconds.

## Config tree — burn, releases, and escalation in one place

Five folders — `burn`, `releases`, `escalation`, `budget`, `audit`:

```yaml
burn/
  checkout/
    max_rate_1h: 6.0
    max_rate_2h: 3.0
    max_rate_6h: 1.5
    block_promote_above: 3.0
  search/
    max_rate_2h: 4.0
    block_promote_above: 4.0
releases/
  pause_canary: false
  pause_all_prod: false
  allowed_services_during_pause: ["hotfix-only"]
  max_canary_weight_pct: 50
escalation/
  budget_remaining_floor_pct: 15
  notify_director: false
  page_oncall_tier: 2
budget/
  checkout/
    remaining_pct: 38
    window_days_left: 18
    last_synced_at_ms: 0
audit/
  last_pause_by: ""
  last_pause_at_ms: 0
  last_threshold_change_by: ""
```

One tree. One profile path: `['platform']['sre']['prod']['live']`. Staging rehearses **identical key layout** — only values differ.

## Java integration: release gate + live burn ceiling

```java
import io.kiponos.sdk.Kiponos;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

@Service
public class SloAwareReleaseGate {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();
    private final SloMetricsClient sloClient;
    private final DeployService deployService;

    public SloAwareReleaseGate(SloMetricsClient sloClient, DeployService deployService) {
        this.sloClient = sloClient;
        this.deployService = deployService;
        kiponos.afterValueChanged(change ->
            log.info("SLO policy delta: path={} value={}", change.path(), change.newValue())
        );
    }

    public PromoteResponse promote(PromoteRequest req) {
        var releases = kiponos.path("releases");
        var burn = kiponos.path("burn", req.serviceName());

        if (releases.getBool("pause_canary") || releases.getBool("pause_all_prod")) {
            return PromoteResponse.blocked("releases paused by SRE policy");
        }

        double liveBurn2h = sloClient.burnRate2h(req.serviceName());
        float ceiling = burn.getFloat("block_promote_above");
        int maxWeight = releases.getInt("max_canary_weight_pct");

        if (liveBurn2h > ceiling) {
            return PromoteResponse.blocked(
                "burn_rate_2h=%.2f exceeds live ceiling %.2f".formatted(liveBurn2h, ceiling)
            );
        }

        int target = Math.min(req.getTargetWeight(), maxWeight);
        return deployService.promoteCanary(req.serviceName(), target);
    }

    public void syncBudgetRemaining(String service) {
        double remaining = sloClient.budgetRemainingPct(service);
        kiponos.path("budget", service).set("remaining_pct", remaining);

        var esc = kiponos.path("escalation");
        if (remaining < esc.getFloat("budget_remaining_floor_pct")) {
            esc.set("notify_director", true);
        }
    }
}

@RestController
@RequestMapping("/deploy")
public class DeployController {
    private final SloAwareReleaseGate gate;

    @PostMapping("/promote")
    public PromoteResponse promote(@RequestBody PromoteRequest req) {
        return gate.promote(req);
    }
}
```

Every `getFloat()`, `getBool()`, and `getInt()` on the promote path is **O(1) local cache** — microseconds, not cross-region config service RTT.

Wire a cron or Prometheus adapter to call `syncBudgetRemaining()` so `notify_director` flips live when budget drains — no second source of truth in a spreadsheet.

## Real-world scenarios

| Scenario | Without live burn tree | With Kiponos one-tree SLO policy |
|----------|------------------------|----------------------------------|
| Fast burn during canary | Alert pages; promote API still accepts 50% | Flip `releases.pause_canary: true` in dashboard |
| Checkout stricter than search | One global `MAX_BURN_RATE` blocks wrong service | Per-service `burn/checkout/block_promote_above` |
| Director escalation at 15% remaining | Manual Slack thread | `escalation/notify_director` flips on sync job |
| Recovery — resume releases | Second Helm wave | Single edit: `pause_canary: false`, restore ceiling |
| Game day burn inject | Staging uses different env var names | Same tree shape; rehearsal flips real keys |

## Performance: why release gates must not add network I/O

- **One WebSocket per JVM** — not one config fetch per promote request
- **Burn ceiling check is two local reads** — nanoseconds vs deploy orchestrator I/O
- **Delta patches** — tightening `block_promote_above` sends one patch, not full tree reload
- **Budget sync writes via SDK** — optional background job, zero impact on promote hot path
- **No GC pressure** from re-parsing YAML on every CI trigger during incident load

In load tests, Kiponos reads are noise on the promote path; Argo CD and Kubernetes API latency dominate.

## Compare to alternatives

| Approach | Mid-incident threshold tighten | Hot-path read latency | Single tree for burn + pause + escalation |
|----------|-------------------------------|----------------------|-------------------------------------------|
| Grafana alert only | No — informs humans only | N/A | No — scattered artifacts |
| Helm values for gates | No — pipeline bound | Zero (static) but stale | Partial — no live flip |
| Redis config hash | Yes with poll | Poll interval adds tail latency | Possible — custom schema discipline |
| Feature-flag SaaS | Booleans only | SDK network on evaluation | No — float thresholds awkward |
| `@RefreshScope` Spring | Bean recycle — drops in-flight | Zero after refresh storm | Partial — still restart-like blast |
| **Kiponos SDK** | **Yes — dashboard delta** | **Zero (in-process cache)** | **Yes — one profile tree** |

## When not to use Kiponos for error budget policy

| Boundary | Better home |
|----------|-------------|
| SLI definition, PromQL recording rules, raw time-series | Prometheus / Mimir — source metrics |
| Historical burn postmortems and monthly SLO reports | Data warehouse / BI |
| Immutable service identity and deploy artifact digests | GitOps → cluster reconcile |
| Secrets for PagerDuty API keys | Vault / sealed-secrets — not live dashboard |
| Regulatory audit of who deployed what binary | CI/CD immutable audit log |

## Getting started (15 minutes)

1. [Create TeamPro at kiponos.io](https://kiponos.io) — add profile `['platform']['sre']['prod']['live']` with `burn`, `releases`, and `escalation` folders matching the tree above.
2. Add `io.kiponos:sdk-boot-3` to your Spring Boot release-gate or platform service.
3. Set `KIPONOS_ID`, `KIPONOS_ACCESS`, and `-Dkiponos="['platform']['sre']['prod']['live']"`.
4. Replace `MAX_BURN_RATE` constants with `kiponos.path("burn", service).getFloat("block_promote_above")`.
5. Register `afterValueChanged` logging and wire `syncBudgetRemaining()` from your metrics adapter.
6. Game day: in staging, inject synthetic burn and flip `pause_canary: true` — confirm promote API blocks **without pod restart**. Document key names in your SLO runbook.

## Further reading

- [Developer Quickstart](https://dev.to/kiponos/kiponosio-developer-quickstart-java-python-and-your-first-live-config-change-3kjo)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
- Related: [Canary traffic weights](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-canary-traffic-weights.md)
- Related: [OpenTelemetry SLO thresholds](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-opentelemetry-slo-thresholds.md)

---

*Kiponos.io — your burn alert pages humans; the tree pauses the pipeline.*