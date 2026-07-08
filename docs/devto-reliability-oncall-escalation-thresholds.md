---
title: "On-Call Escalation Thresholds Live — Tune Paging Before Bridge Overflows (Java SDK)"
published: false
tags: java, sre, observability, devops
description: PagerDuty thresholds in Terraform are not bridge knobs. Kiponos holds escalation timing and severity cutoffs ops tunes live.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-reliability-oncall-escalation-thresholds.md
main_image: https://files.catbox.moe/87bzmo.jpg
---

Monday 07:12 UTC. A misconfigured canary deployment trips **47 low-severity alerts** in nine minutes — all technically "correct" but none customer-impacting. Primary on-call acknowledges each page. The bridge fills with duplicate context. Secondary on-call gets escalated at minute fifteen for every unacknowledged noise page, per policy.

The escalation policy lives in Terraform beside PagerDuty service definitions:

```hcl
escalation_rule {
  escalation_delay_in_minutes = 15
}
```

Java-side, your internal escalation coordinator mirrors the constant:

```java
private static final int ESCALATE_AFTER_MIN = 15;
```

The SRE manager asks the question that stops the scroll:

> "Can we lengthen escalation to **thirty minutes** for this false-positive wave without a Terraform apply and a schedule redeploy?"

Terraform owns **who** gets paged. The **minutes_before_escalate** float is an **incident bridge knob** — it should move while JVMs keep evaluating alert state.

[Kiponos.io](https://kiponos.io) holds escalation thresholds under `['oncall']['prod']['escalation']` — local `getInt()` on every evaluation tick, `afterValueChanged` to reset pending escalation timers when ops adjusts the window.

## The problem — minutes_before_escalate baked into static config

Your on-call coordinator service tracks open incidents and schedules secondary pages:

```java
@Service
public class EscalationCoordinator {

    private static final int ESCALATE_AFTER_MIN = 15;

    @Scheduled(fixedRate = 60_000)
    public void evaluateOpenIncidents() {
        for (OpenIncident incident : incidentStore.unacknowledged()) {
            if (incident.minutesOpen() >= ESCALATE_AFTER_MIN) {
                pagerDutyClient.escalate(incident.id(), EscalationLevel.SECONDARY);
            }
        }
    }
}
```

During a false-positive wave, you need `minutes_before_escalate: 30` **now** — not after Terraform plan/apply, not after recycling the coordinator pods while pages still fire.

Mixing this float in PagerDuty Terraform is awkward: you are not changing **who** is on-call, you are changing **how long primary gets** before secondary inherits noise.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Escalation policy belongs in PagerDuty/Terraform" | PD owns schedules; **timing floats** are bridge decisions |
| "Primary should ack faster during noise" | Humans fatigue; lengthening window reduces cascade pages |
| "Silence rules fix false positives" | Silences hide the next real alert in the same service |
| "We will tune Terraform after the incident" | Secondary gets paged 12 times before apply finishes |
| "Internal coordinator duplicates PD — pick one" | Coordinator adds severity logic PD cannot express |

## The Aha

**minutes_before_escalate is operational config** — it changes during false-positive waves, drill weekends, and major-incident bridge mode. It belongs in a **live tree** the coordinator reads with `getInt()` every minute, not in a `static final` compiled beside Terraform.

## What Kiponos.io is for escalation tuning

Profile `['oncall']['prod']['escalation']` syncs `minutes_before_escalate`, severity overrides, and bridge-mode flags into every coordinator JVM. Dashboard edit sends a **delta**; the next scheduled evaluation uses the new window.

`kiponos.path("escalation", "timing").getInt("minutes_before_escalate")` is a **local memory read** on the minute tick — no HTTP to PagerDuty API for policy, no Terraform state pull.

`afterValueChanged` clears pending escalation deadlines when ops flips `false_positive_wave_mode` — coordinators recalculate without pod restart.

Honest boundary: Kiponos does **not** replace PagerDuty schedules, on-call rotations in Terraform, or alert routing rules in Prometheus. It owns **timing floats** your Java coordinator enforces between primary ack and secondary page.

## Architecture

![Architecture diagram](https://files.catbox.moe/gtla1s.png)

## Config tree

```yaml
escalation/
  timing/
    minutes_before_escalate: 15
    minutes_before_executive: 45
    enabled: true
  severity/
    low/
      minutes_before_escalate: 30
    high/
      minutes_before_escalate: 5
    critical/
      minutes_before_escalate: 2
  bridge/
    false_positive_wave_mode: false
    wave_minutes_before_escalate: 30
    auto_expire_minutes: 60
  ops/
    owner: sre-manager
    notes: "Lengthen during canary noise — never during customer-impacting SEV1"
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
public class EscalationCoordinator {

    private final Kiponos kiponos;
    private final IncidentStore incidentStore;
    private final PagerDutyClient pagerDutyClient;
    private final Map<String, Instant> pendingEscalation = new ConcurrentHashMap<>();

    public EscalationCoordinator(Kiponos kiponos, IncidentStore incidentStore,
                                 PagerDutyClient pagerDutyClient) {
        this.kiponos = kiponos;
        this.incidentStore = incidentStore;
        this.pagerDutyClient = pagerDutyClient;
        kiponos.afterValueChanged(this::onEscalationConfigChange);
    }

    @Scheduled(fixedRate = 60_000)
    public void evaluateOpenIncidents() {
        for (OpenIncident incident : incidentStore.unacknowledged()) {
            int thresholdMin = resolveEscalationMinutes(incident.severity());
            Instant deadline = pendingEscalation.computeIfAbsent(
                    incident.id(),
                    id -> incident.openedAt().plus(Duration.ofMinutes(thresholdMin)));
            if (Instant.now().isAfter(deadline)) {
                pagerDutyClient.escalate(incident.id(), EscalationLevel.SECONDARY);
                pendingEscalation.remove(incident.id());
            }
        }
    }

    private int resolveEscalationMinutes(String severity) {
        if (kiponos.path("escalation", "bridge")
                .getBool("false_positive_wave_mode", false)) {
            return kiponos.path("escalation", "bridge")
                    .getInt("wave_minutes_before_escalate", 30);
        }
        var sev = kiponos.path("escalation", "severity", severity);
        int override = sev.getInt("minutes_before_escalate", 0);
        if (override > 0) {
            return override;
        }
        return kiponos.path("escalation", "timing")
                .getInt("minutes_before_escalate", 15);
    }

    private void onEscalationConfigChange(ValueChange change) {
        if (change.path().startsWith("escalation/")) {
            pendingEscalation.clear();
            log.warn("Escalation policy changed: {} → {} — pending timers reset",
                    change.path(), change.newValue());
        }
    }
}
```

Ops enables `false_positive_wave_mode` during the canary noise. Secondary pages slow from fifteen to thirty minutes — primary gets room to ack and dismiss without cascade fatigue.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| False positive wave — lengthen escalation window | Terraform apply + coordinator redeploy | `false_positive_wave_mode` live |
| SEV1 customer impact — shorten window | Emergency Terraform PR | `severity/critical/minutes_before_escalate: 2` |
| Weekend drill — relaxed timing | Schedule override in PD | Hub profile `drill/escalation` |
| Wave ends — restore defaults | Second Terraform apply | Disable `false_positive_wave_mode` |
| Audit "who widened escalation at 07:14?" | Git blame on `.tf` files | Hub change log with actor |

## Performance on the coordinator tick

- **`getInt()` once per incident per minute** — local reads; noise vs PagerDuty API RTT
- **`afterValueChanged` clears map once** — not per incident evaluation
- **One WebSocket per coordinator JVM** — not Terraform state fetch per tick
- **Delta patch** — minutes 15 → 30 sends one integer
- **Severity nested overrides** — low-severity noise without touching critical path

## Compare to alternatives

| Approach | Lengthen escalation during noise | Hot-path read cost | Severity-specific windows |
|----------|----------------------------------|-------------------|---------------------------|
| PagerDuty Terraform only | Apply + propagate delay | N/A | One rule per service |
| PagerDuty manual override | Per-incident clicks | N/A | Does not scale to 47 pages |
| Spring Cloud Config | Refresh + recycle | Network on refresh | Flat keys |
| Hard-coded Java constant | Redeploy coordinator | Zero (frozen) | Requires code change |
| **Kiponos live hub** | **Seconds** | **Local get*()** | **Nested severity tree** |

## When not to use Kiponos

| Case | Use instead |
|------|-------------|
| On-call rotation schedules | PagerDuty + Terraform |
| Who receives pages at each level | Escalation policy in PD |
| Alert routing rules (which metric fires) | Prometheus/Alertmanager Git |
| Executive escalation phone tree | HR + ops runbook |
| Bootstrap coordinator wiring | Git-reviewed Spring config |

## Getting started (15 minutes)

1. Sign up at [kiponos.io](https://kiponos.io) (TeamPro).
2. Create profile path `['oncall']['prod']['escalation']`.
3. Add `io.kiponos:sdk-boot-3` to on-call coordinator service.
4. Set `KIPONOS_ID`, `KIPONOS_ACCESS`, and `-Dkiponos="['oncall']['prod']['escalation']"`.
5. Move `minutes_before_escalate` out of Java constants into `escalation/timing/`.
6. Wire `EscalationCoordinator` with `afterValueChanged` pending timer reset.
7. Staging game day: inject low-severity alert flood, enable `false_positive_wave_mode`, confirm secondary pages delay **without pod restart**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [SLO burn multiplier live](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-reliability-slo-burn-multiplier-live.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*minutes_before_escalate belongs in the live ops tree — not in Terraform that applies slower than your bridge fills.*