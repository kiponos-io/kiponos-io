---
title: "The Mind Reader Demo Trick — Same Zero-Latency Config That Changes ICU Parameters Live (Spring Boot)"
published: false
tags: java, springboot, architecture, devops
description: A Spring Boot sales demo that "reads minds" is the same Kiponos pattern ICU services use to change ventilation parameters mid-loop — WebSocket deltas, in-memory reads, no restart.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-mind-reader-live-ops.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-mind-reader-live-ops.jpg
---

The investor leans forward. You ask for their kid's pet name — something only they would know. Your colleague, laptop half-closed, types it into a dashboard. You announce that your platform is *extremely* adaptive.

The next screen refresh shows **"Welcome back, Mr. Whiskers"** in the hero banner — bold, centered, as if it had been hard-coded since sprint zero.

The room laughs. Someone asks if you built a mind-reading LLM.

You did not. You wired [Kiponos.io](https://kiponos.io) into a Spring Boot demo: **WebSocket delta updates → in-memory SDK cache → local `get()` on every render**. The running JVM never restarted. The name was not in Git five minutes ago.

That same read path — zero network on the hot loop — is how hospital services change **operational parameters** while monitoring code keeps running. Funny opening. Serious production pattern.

## The painful problem both scenes share

Whether you are pitching or running an ICU gateway service, you hit the same wall:

| Moment | What teams try | Why it fails |
|--------|----------------|--------------|
| Live demo | Hard-code personalization; redeploy between meetings | Cannot adapt in the room |
| Launch-day flag | Emergency PR + pod rollout | Pipeline latency kills the moment |
| Clinical ops change | Restart Java service to pick up new YAML | Downtime or dropped monitoring cycles |
| Tight control loop | Poll Redis/REST inside `while` | Network RTT in the hottest path |

What you need:

- Values **already local** when the loop or request handler runs
- Updates via **WebSocket deltas** — not reloading a config blob
- **No restart** — the process keeps its state; only the numbers change

Kiponos does exactly that for Java and Python SDKs.

## How it works (no witchcraft, just architecture)

![Architecture diagram](https://files.catbox.moe/al8ksu.png)

1. **Connect once** at startup — `Kiponos.createForCurrentTeam()` opens `wss://kiponos.io/api/io-kiponos-sdk`.
2. **Initial tree load** — full snapshot for your profile path.
3. **Delta updates** — change one key; only that node patches in memory.
4. **Reads are local** — `kiponos.path(...).get_*()` returns cached values. No HTTP on the read path.

The demo screen and the ICU monitoring loop do not block on config. A background WebSocket worker merges deltas. The next iteration sees fresher numbers.

## Act 1 — The mind reader demo (Spring Boot)

Organize demo personalization under one profile folder:

```yaml
demo/
  personalization/
    display_name: "Welcome"
    subtitle: "Your adaptive platform"
    show_confetti: false
  features/
    use_production_scoring: false   # the TODO that survived staging
```

### Bootstrap in Spring Boot 3

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

### Controller — local reads every request

```java
@RestController
public class DemoDashboardController {

    private final Kiponos kiponos;

    public DemoDashboardController(Kiponos kiponos) {
        this.kiponos = kiponos;
    }

    @GetMapping("/api/demo/hero")
    public HeroPayload hero() {
        var personal = kiponos.path("demo", "personalization");
        return new HeroPayload(
                personal.getString("display_name", "Welcome"),
                personal.getString("subtitle", ""),
                personal.getBool("show_confetti", false)
        );
    }
}
```

While the investor watches, your teammate sets `demo/personalization/display_name` to `"Mr. Whiskers"` in the Kiponos dashboard. The front end polls or streams `/api/demo/hero` — the next response already contains the name. **No `@RefreshScope`. No actuator restart. No "give me five minutes to redeploy."**

Optional audit hook:

```java
kiponos.afterValueChanged(change ->
        log.info("[kiponos] {} → {}", change.getPath(), change.getNewValue()));
```

Keep callbacks lightweight; the hot path stays `get()` from cache.

### What you are really demonstrating

| Audience reaction | Under the hood |
|-------------------|----------------|
| "How did it know?!" | Dashboard edit → delta → SDK memory |
| "Is this AI?" | Typed config tree, not a model |
| "Can prod do this?" | Same SDK contract on payment, triage, ICU paths |

**Be honest in the room:** say it is live operational config, not telepathy. The trick lands harder when engineers realize it is production-grade.

## Act 2 — Same SDK, ICU operational loop (emotional stakes)

The laugh fades. The architecture does not.

Hospital gateways and protocol services run **tight loops** — vitals ingestion, alarm evaluation, ventilation protocol steps. Attendings order parameter changes *now*, not after the next deployment window.

You already tune [triage routing at runtime](https://dev.to/kiponos/change-hospital-triage-routing-rules-at-runtime-no-java-redeploy-kiponos-sdk). The same pattern applies to **approved operational knobs** on a monitoring path:

```java
public VentilationAdvice evaluate(VitalsSnapshot vitals) {
    var protocol = kiponos.path("icu", "ventilation");
    double peep = protocol.getFloat("peep_cm_h2o");
    int fio2 = protocol.getInt("fio2_percent");
    boolean fastWean = protocol.getBool("fast_wean_protocol_active");

    // Clinical logic uses locally cached operational parameters
    return ventilatorPolicy.advise(vitals, peep, fio2, fastWean);
}
```

`getFloat()` / `getInt()` / `getBool()` are **in-process reads** — safe inside a loop that runs every second (or faster). When biomedical engineering or the charge nurse updates `peep_cm_h2o` from the hub, the **already-running** service picks it up on the next evaluation. No pod recycle. No "turn monitoring off and on again."

### Operational config tree (ICU profile)

```yaml
icu/
  ventilation/
    peep_cm_h2o: 8.0
    fio2_percent: 40
    fast_wean_protocol_active: false
  alarms/
    spo2_low_threshold: 92
    hr_high_threshold: 130
    suppression_active: false
  routing/
    overflow_to_stepdown: false
```

### Real operational changes (same mechanism as the demo)

| Event | Live config change | Read path |
|-------|-------------------|-----------|
| Attending adjusts PEEP | `peep_cm_h2o` 8 → 10 | Next `evaluate()` cycle |
| Night shift alarm fatigue | Toggle `suppression_active` with audit | Next alarm check |
| Ward surge | Enable `overflow_to_stepdown` | Next routing call |
| Flu protocol | Mirror triage `flu_season_mode` flags | Shared hub profile |

**Compliance note:** Kiponos holds **operational thresholds and protocol flags** approved by your clinical workflow — not replacement for regulated device firmware or bedside clinician judgment. Pair with your audit log; use `afterValueChanged` for change records. The innovation is **latency and uptime**, not bypassing governance.

## Act 3 — The launch-day toggle (honest developer humor)

Every team has shipped with a shameful flag still on. In the demo tree:

```java
boolean prodScoring = kiponos.path("demo", "features")
        .getBool("use_production_scoring", false);

return prodScoring ? productionScorer.score(event) : demoScorer.score(event);
```

Deploy pipeline backed up 45 minutes. Investor still in the building. Flip `use_production_scoring` to `true` from the parking lot. The **same JVM** starts scoring for real — because the request path reads live memory, not frozen YAML.

That is the toggle aspect: not a feature-flag SaaS round-trip — a **local boolean** on the hot path, changed with full audit in the hub.

## Performance — why there is no hit

| Concern | Reality |
|---------|---------|
| Network per read | None — dictionary lookup on cached tree |
| WebSocket overhead | One connection per process lifetime |
| Update cost | Delta patch of one key, async merge |
| Spring `@RefreshScope` | Avoided — no bean recycle for a float |

On a saturated Spring service, the bottleneck remains your business logic and I/O — not `kiponos.path(...).getFloat(...)`.

## Before / after

| Approach | Mid-demo / mid-shift change | Read latency | Ops story |
|----------|----------------------------|--------------|-----------|
| Hard-coded constants | Redeploy | Zero after redeploy | Embarrassing |
| `@RefreshScope` + actuator | Context refresh | Zero after refresh | Bean churn under load |
| Poll Redis/REST in loop | Yes | RTT per poll | Fragile hot path |
| **Kiponos SDK** | **Yes** | **Zero (local cache)** | **Dashboard + audit** |

## When not to use Kiponos

| Use case | Better home |
|----------|-------------|
| Infrastructure desired state (replicas, Ingress) | GitOps / Helm |
| Long-lived secrets | Vault / sealed-secrets |
| Boolean experiments with audience segmentation | Feature-flag product |
| Replacing regulated device firmware | Vendor-controlled medical devices |
| "Mind reading" without disclosure | **Do not** — show the dashboard, keep trust |

## Getting started

1. **Free TeamPro** at [kiponos.io](https://kiponos.io) — create `demo` and `icu` profile folders (or env-separated paths).
2. Add personalization and protocol trees in the dashboard.
3. Wire the [Java SDK](https://github.com/kiponos-io/kiponos-io) with team id, access key, and profile path.
4. Replace hard-coded demo strings and operational floats with `kiponos.path(...).get_*()`.
5. Run the mind reader trick once. Then run a game day: change `peep_cm_h2o` while the loop logs prove the next tick picked it up.

Open-source integration: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io) — golden Java example, Python SDK, Agent Skills.

---

*Kiponos.io — funny in the demo room. Serious in the ICU gateway. Same zero-latency read path.*