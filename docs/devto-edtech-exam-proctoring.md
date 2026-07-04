---
title: "Raise Remote Exam Proctoring Flag Thresholds During a Cheating Surge — No Java Restart (Kiponos SDK)"
published: false
tags: java, edtech, security, education
description: Change suspicion score thresholds, review queue caps, and integrity pause flags in Java proctoring services while exams stay in session. Kiponos local reads on every behavior evaluation.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-edtech-exam-proctoring.md
main_image: https://litter.catbox.moe/q99m57.jpg
---

Finals week hour 2. Your integrity dashboard shows a **340% spike** in tab-switch events across the organic chemistry cohort. The proctoring service still flags sessions at `suspicion_score_threshold: 0.72` — unchanged since the pilot semester when false positives were the bigger risk.

The academic integrity lead messages platform engineering:

> "Tighten flags to **0.65** for CHEM-201 and **pause auto-submit** until reviewers catch up — students are still testing."

A config PR means restarting proctoring pods during active exam windows. Every minute of frozen thresholds is either cheaters slipping through or legitimate students flooding appeals.

**`suspicion_score_threshold` is not pedagogy — it is this exam week's integrity dial.**

[Kiponos.io](https://kiponos.io) lets academic ops move proctoring policy **while exam sessions keep running** — WebSocket deltas, in-memory reads on every behavior scoring event.

## The problem: static thresholds on the proctoring hot path

```java
@Service
public class LegacyProctoringEvaluator {
    @Value("${proctoring.suspicion_score_threshold:0.72}")
    private double suspicionThreshold;

    @Value("${proctoring.auto_flag_tab_switch_count:4}")
    private int tabSwitchFlagCount;

    public ProctoringAction evaluate(ExamSession session, BehaviorEvent event) {
        double score = session.aggregateSuspicionScore();
        if (score >= suspicionThreshold) {
            return ProctoringAction.flagForReview("score_exceeded");
        }
        if (event.tabSwitchCount() >= tabSwitchFlagCount) {
            return ProctoringAction.flagForReview("tab_switch");
        }
        return ProctoringAction.allow();
    }
}
```

Proctoring thresholds usually come from:

1. **YAML at semester start** — cheating patterns shift mid-exam week
2. **Feature-flag boolean** — still need numeric score floors somewhere
3. **DB poll per event** — unacceptable latency on WebRTC behavior streams

| What teams say | What production does |
|----------------|---------------------|
| "Thresholds are set by faculty committee" | Committee met ≠ JVM restarted |
| "We'll run a stricter profile next semester" | This semester's exam is **today** |
| "ML model handles integrity" | Model outputs still pass through **policy gates** |
| "False positives hurt vulnerable students" | Ops needs to **loosen or tighten** live, not debate in Git |

## The Aha: suspicion thresholds are operational integrity policy

Store proctoring ops config under `proctoring/exams` in Kiponos. Each `evaluate()` reads course-specific `suspicion_score_threshold`, `tab_switch_flag_count`, and review queue caps from the in-memory tree. When integrity ops lowers CHEM-201's threshold to `0.65`, the **next** behavior event sees it — no service restart.

Kiponos controls **operational thresholds and pause flags**, not the underlying behavior detection models or FERPA data handling.

## What is Kiponos.io — for edtech proctoring

Kiponos connects your Spring Boot proctoring service to a live config tree. Profile `['edtech']['prod']['proctoring']` hydrates at startup. Dashboard edits are **WebSocket deltas**. `kiponos.path("proctoring", "exams", courseCode).getFloat("suspicion_score_threshold")` is a **local read** — no remote call on every gaze/track/keyboard event.

## Architecture

![Architecture diagram](https://litter.catbox.moe/lf7uq6.png)

## Example config tree

```yaml
proctoring/
  exams/
    CHEM_201/
      suspicion_score_threshold: 0.72
      tab_switch_flag_count: 4
      auto_submit_enabled: true
      review_queue_cap: 200
    MATH_101/
      suspicion_score_threshold: 0.78
      tab_switch_flag_count: 6
      auto_submit_enabled: true
    default/
      suspicion_score_threshold: 0.75
      tab_switch_flag_count: 5
  global/
    integrity_surge_mode: false
    halt_auto_submit: false
    escalate_to_live_proctor_score: 0.85
  review/
    max_pending_flags: 500
    priority_course_csv: CHEM_201,PHYS_220
```

## Bootstrap and integration (Spring Boot 3)

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
public class KiponosProctoringEvaluator {

    private final Kiponos kiponos;

    public KiponosProctoringEvaluator(Kiponos kiponos) {
        this.kiponos = kiponos;
        kiponos.afterValueChanged(change -> {
            if (change.path().startsWith("proctoring/")) {
                log.info("Proctoring policy changed: {} → {}", change.path(), change.newValue());
            }
        });
    }

    public ProctoringAction evaluate(ExamSession session, BehaviorEvent event) {
        var global = kiponos.path("proctoring", "global");
        if (global.getBool("halt_auto_submit", false)) {
            return ProctoringAction.hold("auto_submit_halted");
        }

        String courseKey = session.courseCode().replace("-", "_");
        var policy = kiponos.path("proctoring", "exams", courseKey);
        if (!policy.exists()) {
            policy = kiponos.path("proctoring", "exams", "default");
        }

        double threshold = policy.getFloat("suspicion_score_threshold", 0.75);
        if (global.getBool("integrity_surge_mode", false)) {
            threshold -= 0.05; // tighter during documented surge
        }

        double score = session.aggregateSuspicionScore();
        if (score >= threshold) {
            return ProctoringAction.flagForReview("score_exceeded");
        }

        int tabLimit = policy.getInt("tab_switch_flag_count", 5);
        if (event.tabSwitchCount() >= tabLimit) {
            return ProctoringAction.flagForReview("tab_switch");
        }

        double liveEscalate = global.getFloat("escalate_to_live_proctor_score", 0.85);
        if (score >= liveEscalate) {
            return ProctoringAction.escalateLiveProctor();
        }
        return ProctoringAction.allow();
    }
}
```

Every `getFloat()` is a **local memory read** — safe on behavior streams that process thousands of events per active session.

## Real scenarios

| Event | Frozen YAML reflex | Kiponos path |
|-------|-------------------|--------------|
| Cheating ring detected | Emergency deploy mid-exam | `proctoring/exams/CHEM_201/suspicion_score_threshold: 0.65` |
| Review queue meltdown | Students auto-failed | `proctoring/global/halt_auto_submit: true` |
| False positive backlash | Semester-long branch | Loosen `MATH_101` threshold live with audit |
| High-stakes licensure exam | Stricter global mode | `proctoring/global/integrity_surge_mode: true` |
| Live proctor staffing gap | Raise escalation bar | Bump `escalate_to_live_proctor_score` |

## Performance — why proctoring streams stay real-time

- One WebSocket per proctoring JVM — not one config fetch per behavior event
- `getFloat("suspicion_score_threshold")` is O(1) on the cached tree
- Delta updates — course threshold change sends one patch
- WebRTC worker threads never block on integrity ops database
- `afterValueChanged` audit when academic ops moves thresholds during active finals

## Compare to alternatives

| Approach | Tighten flags during cheating surge | Per-event read cost | Course-specific policy |
|----------|-------------------------------------|---------------------|------------------------|
| `proctoring.yml` at semester start | PR + deploy | Zero (frozen) | Code branches |
| LMS admin manual settings | Disconnected from Java scorer | N/A | Per-course UI only |
| Poll integrity DB | Possible | DB RTT × event volume | Schema coupling |
| **Kiponos SDK** | **Dashboard (seconds)** | **Memory read** | **Folder per course** |

## When not to use Kiponos for proctoring

| Case | Better approach |
|------|-----------------|
| Behavior detection ML model weights | Model training / validation pipeline |
| Student identity verification vendor | Procurement integration |
| FERPA data retention policy | Legal compliance baseline |
| Replacing proctoring vendor entirely | RFP / migration project |

## Getting started (15 minutes)

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['edtech']['prod']['proctoring']`.
2. Add `io.kiponos:sdk-boot-3` to your proctoring service.
3. Create `proctoring/exams/CHEM_201` with `suspicion_score_threshold`, `tab_switch_flag_count`.
4. Replace `@Value` threshold reads with `kiponos.path(...)`.
5. Game day: replay behavior events in staging, lower threshold live, re-evaluate — flag decision changes **without pod restart**.

**Further reading:**

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for Java edtech. Tighten integrity while exams stay live.*