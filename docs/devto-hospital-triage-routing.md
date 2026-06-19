---
title: "Change Hospital Triage Routing Rules at Runtime — No Java Redeploy (Kiponos SDK)"
published: true
tags: java, healthcare, ops, realtime
description: Update queue routing, acuity thresholds, and department capacity rules in live Java triage systems without downtime. Kiponos delivers local zero-latency config reads.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-hospital-triage-routing.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-hospital-triage.jpg
---

Emergency departments change protocols when flu season hits, a trauma surge arrives, or a ward goes offline. Operational rules should not wait for a **scheduled Java deployment**.

[Kiponos.io](https://kiponos.io) gives triage and routing services **live operational config** — WebSocket deltas, in-memory reads on every patient event.

## Routing hot path

```java
public Department route(PatientEvent event) {
    var rules = kiponos.path("triage", "routing");
    int acuity = scoreAcuity(event);
    if (acuity >= rules.getInt("trauma_threshold")) {
        return Department.TRAUMA;
    }
    if (rules.getBool("pediatric_overflow_to_general")) {
        return Department.PEDIATRIC;
    }
    return Department.GENERAL;
}
```

`getInt()` / `getBool()` are local — safe on the admission event path.

## Operational config tree

```
triage/
  routing/
    trauma_threshold: 8
    pediatric_overflow_to_general: false
    fast_track_enabled: true
  capacity/
    ed_max_queue: 45
    redirect_non_urgent: true
  protocols/
    flu_season_mode: false
    mask_required: false
```

## Real operational changes

| Event | Live config change |
|-------|-------------------|
| Flu surge | Enable `flu_season_mode`, adjust routing |
| Ward closure | Redirect overflow flags |
| Staffing shortage | Lower `ed_max_queue`, enable redirect |
| Policy update | Toggle `fast_track_enabled` |

Clinical code stays deployed. **Operational parameters** change in Kiponos — auditable, instant, no pod restart.

## Compliance note

Kiponos controls **operational thresholds and routing flags**, not clinical decision models. Pair with your existing audit logging; use `afterValueChanged` listeners for change records.

Free TeamPro: [kiponos.io](https://kiponos.io). Integration: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for Java. Adapt operations while the ED is live.*