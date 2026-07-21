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


## Deeper design notes

### Why “always on at 2s” fails socially

Leak detection that always logs becomes wallpaper. On-call stops reading it. Then the one real leak of the quarter is ignored for three days. Instrument mode — **loud for a hunt, quiet for cruise** — matches how humans actually work.

### Pair with pool metrics

Leak stacks without pool metrics are incomplete. Watch:

| Signal | Meaning |
|--------|---------|
| Active connections | Borrowed now |
| Pending threads | Waiters |
| Connection timeout count | Failures to acquire |
| Leak log rate | Suspicion density after dial-up |

Raise leak threshold only when active/pending disagree with “we close everything.”

### Multi-datasource estates

Billing DB and catalog DB are not the same patient:

```text
jdbc/
  billing/
    hikari/
      leak-detection-threshold-ms
  catalog/
    hikari/
      leak-detection-threshold-ms
```

Hunt the pool that is lying; leave the quiet pool quiet.

### Spring Boot caveats

Some versions expose leak threshold only at config construction. Know your stack:

1. Prefer MXBean / documented runtime setters when available.  
2. If your version cannot reconfigure live, still store the dial in Kiponos and apply on a **controlled recycle** of that datasource only — better than a full app redeploy if your platform supports it.  
3. Document which path you are on in the runbook so 3am does not invent science.

## Field checklist

- [ ] Default threshold `0` in code  
- [ ] Kiponos key documented for each datasource  
- [ ] on_change or tick applies without full restart when possible  
- [ ] Log volume alert if leak logs explode  
- [ ] Hunt ends with dial-down  

## Closing

Leak detection that requires a release is archaeology. Leak detection you can arm for twenty minutes is **incident science**. Put the dial where on-call’s hands already are.

## What teams believe vs reality

| Belief | Production reality |
|--------|--------------------|
| "Leak detection is a startup flag" | It is a **temporary microscope** |
| "Always-on 2s is fine" | Log noise trains people to ignore stacks |
| "We'll enable it next release" | The leak is happening **this hour** |
| "Restart will help us see it" | Restart **erases** the hanging borrow |

## What is Kiponos.io (in this story)

Kiponos is a live nested config hub. Ops changes a value on the dashboard; the Java SDK receives a **WebSocket delta** and updates an **in-process tree**. Your code calls `getInt(...)` from memory — the SQL hot path does not pay network RTT for every query.

Profile shape (Connect screen):

```text
['payments-api']['1.4.2']['prod']['base']
```

Under that profile you park operational trees like `jdbc/hikari/...` so on-call can find them without spelunking YAML in a deploy ticket.

## Config tree (example)

```yaml
jdbc:
  billing:
    hikari:
      leak-detection-threshold-ms: 0
      # ops may set 5000 during a hunt
  catalog:
    hikari:
      leak-detection-threshold-ms: 0
```

Eight keys is not required for a thin datasource story — but multi-pool estates should not share one global integer if only one pool is guilty.
