---
title: "Hikari leakDetectionThreshold Was a Postmortem Setting — We Made It a Live Incident Dial"
published: false
tags: java, hikari, jdbc, devops
description: HikariCP leakDetectionThreshold is often set once in application.yml and forgotten. During connection-pool mysteries, that threshold should be ops-tunable — Kiponos feeds live leak detection without restarting the pool.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-hikari-leak-detection-threshold.md
main_image: https://files.catbox.moe/xvnh3m.jpg
---

# Hikari `leakDetectionThreshold` Was a Postmortem Setting — We Made It a Live Incident Dial

Connection pool graphs are polite until they are not. Active connections climb. Threads wait. Someone says “maybe a leak.” Someone else says “turn on leak detection.” Someone opens `application.yml` and finds:

```yaml
spring.datasource.hikari.leak-detection-threshold: 0
```

Zero means off. Enabling it means a PR, a deploy, and a prayer that the leak still exists after the restart that destroyed the crime scene.

**The Aha:** leak detection sensitivity is an **incident instrument**, not a build artifact. Hold `leak-detection-threshold-ms` in [Kiponos.io](https://kiponos.io) and apply it to Hikari when ops is actively hunting — then dial it back when the hunt ends.

## The problem: the debugger requires a redeploy

Hikari’s leak detection is valuable: it logs stacks for connections that are borrowed longer than a threshold. But teams treat the threshold like permanent architecture:

| Setting | Typical fate |
|---------|----------------|
| `0` (off) | Quiet production, blind incidents |
| `2000` always on | Noise, log spam, “we ignore leak logs” |
| Change requires restart | Crime scene erased |

You want **off** most days and **sharp** for thirty minutes when the pool is lying to you.

## Architecture: pool stays up; dial moves

| Layer | Job |
|-------|-----|
| **HikariDataSource** | Lives for the process lifetime |
| **SDK cache** | `jdbc/hikari/leak-detection-threshold-ms` |
| **Ops dashboard** | 0 → 5000 during hunt; back to 0 after |
| **Optional admin path** | Soft-apply via `HikariConfigMXBean` / documented reconfigure path for your Spring version |

```text
Ops (Kiponos) ──WS──► SDK cache ──► apply threshold ──► Hikari logs leak stacks
```

## Code sketch: read live, apply carefully

Exact apply APIs differ by Spring Boot / Hikari versions. Pattern:

```java
public final class HikariLeakDial {
    private final HikariDataSource ds;
    private final Kiponos kiponos;
    private volatile int lastApplied = -1;

    public void tick() {
        int ms = kiponos.getInt("jdbc/hikari/leak-detection-threshold-ms", 0);
        if (ms == lastApplied) return;
        // Prefer MXBean / supported reconfigure path for your stack.
        ds.getHikariConfigMXBean().setLeakDetectionThreshold(ms);
        lastApplied = ms;
    }
}
```

Run `tick()` on a slow schedule (e.g. every 5–10s) or on a Kiponos `on_change` handler for that key — **not** on every SQL query.

## Incident playbook

| Step | Action |
|------|--------|
| 1 | Pool exhaustion symptoms (wait, timeout, “connection is not available”) |
| 2 | Set `leak-detection-threshold-ms` to `3000`–`10000` live |
| 3 | Capture leak stack logs for 10–20 minutes |
| 4 | Fix the real borrow/close bug (or the long transaction) |
| 5 | Set threshold back to `0` live |

No “we’ll catch it next deploy when we remember to enable detection.”

## What this is not

| Not this | Why |
|----------|-----|
| Leaving leak detection at 2s forever | Log fatigue; real signal drowns |
| Restarting pods to “refresh config” | Destroys the evidence |
| Feature flag only | You need a **threshold number**, not a boolean alone |

## Why Kiponos fits

Hot-path SQL should not call a remote server. Kiponos keeps the dial in **process memory** with WebSocket updates. Ops changes the number; your tick/on_change applies it; Hikari keeps serving.

People should not have to ship a release to make a decision about how loudly their pool should scream.

---

*Kiponos — live nested config, WebSocket deltas, in-process cache.*  
[kiponos.io](https://kiponos.io) · [GitHub](https://github.com/kiponos-io/kiponos-io)
