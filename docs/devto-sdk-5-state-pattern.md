---
main_image: https://files.catbox.moe/lbowqg.jpg
title: "Kiponos Java SDK 5.0: Real-Time Config That Refuses to Die"
published: false
tags: java, springboot, opensource, devops
description: "SDK 5.0 ships Ready/Offline/Safe modes + Last Known Good — live config that survives disconnects. On Maven Central now."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-sdk-5-state-pattern.md
---

# Kiponos Java SDK 5.0: Real-Time Config That Refuses to Die

Most configuration libraries have two moods.

**Mood one:** everything is fine. You `get("timeout-ms")`, life is good, the dashboard is a distant theory.

**Mood two:** the network hiccups. Your “real-time” client becomes a real-time *exception factory*. Threads hang. Operators refresh. Someone whispers the forbidden words: *“just restart the pod.”*

We built [Kiponos.io](https://kiponos.io) so teams never had to choose between *live* and *safe*. Today we’re shipping the Java SDK release that makes that promise structural — not aspirational.

## **Kiponos Java SDK 5.0.0.260710 is on Maven Central.**

```gradle
repositories { mavenCentral() }
dependencies {
    implementation 'io.kiponos:sdk-boot-3:5.0.0.260710'  // Spring Boot 3 / Jakarta
    // implementation 'io.kiponos:sdk-boot-2:5.0.0.260710'  // Spring Boot 2 / javax
}
```

If you only remember one thing: **5.0 is the state-pattern release.** Ready. Offline. Safe. One facade. No drama.

---

## The problem we refused to ignore

Hot-path services do not care that your config hub had a bad five seconds.

They care that:

- a missing token must not hang the JVM forever  
- a disconnect must not invent random defaults  
- a reconnect must not leave half the fleet on zombie state  

The old “reconnect harder” loop is not a product story. It is a prayer.

So we finished what the architecture always wanted: **modes**, not vibes.

---

## What teams believe vs production reality

| Belief | Production reality |
|--------|--------------------|
| “If the hub is down, fail fast and crash” | Crashing on config I/O is how you turn a blip into an incident |
| “Cache the last value in a HashMap somewhere” | Ad-hoc caches disagree between pods and never get reviewed |
| “We’ll add offline later” | Later is when the outage is already paging you |
| “The SDK can just throw and the app will handle it” | The app *is* busy serving traffic — it shouldn’t re-implement config reliability |

---

## The Aha

**Config availability is an operational mode of the client, not a boolean “connected.”**

When the WebSocket is healthy, you are **Ready** — full live tree, hooks, the works.  
When the connection is gone but you still have a trustworthy snapshot, you are **Offline** — read **Last Known Good**.  
When nothing is trustworthy, you are **Safe** — fail-closed, empty-safe, no silent corruption of the good snapshot.

That is not a retry policy. That is a **state machine with a product contract.**

---

## What is Kiponos.io (in this release’s language)

[Kiponos.io](https://kiponos.io) is a **real-time config hub**:

1. Humans (and agents) edit hierarchical config in a web dashboard.  
2. The server pushes **deltas over WebSocket**.  
3. The Java SDK keeps an **in-memory tree**.  
4. Your hot path calls `get` / `path(...).get(...)` **locally** — zero network on the read.

Profile selection is explicit:

```text
-Dkiponos="['MyApp']['1.0.0']['prod']['base']"
```

Plus Connect tokens:

```text
KIPONOS_ID=…
KIPONOS_ACCESS=…
```

No YAML archaeology. No restart to flip a timeout.

---

## Architecture: the facade that never freezes

```text
Customer app
  └── Kiponos          ← you hold this forever
        └── KiponosBase
              └── SdkState → Ready | Offline | Safe
```

**Hard rule we learned the expensive way:** never return a mode instance to the application.  
If you hand out `OfflineMode`, the caller freezes on Offline for life. The facade switches; the reference stays.

Mode transparency when you need it:

```java
Kiponos k = Kiponos.createForCurrentTeam();

k.isReadyMode();
k.isOfflineMode();
k.isSafeMode();
k.getCurrentMode();
```

---

## Last Known Good (LKG)

Offline is not “hope the heap still has something.”

5.0 persists **Last Known Good** config and reads it back through a deliberate chain (live memory → backup → disk dump). Ready path updates LKG when the live tree is trustworthy. **Safe mode dumps never overwrite LKG** — diagnostics must not poison recovery.

That sentence alone is worth a major version.

---

## Integration (Boot 3)

```java
public class App {
    public static void main(String[] args) {
        Kiponos kiponos = Kiponos.createForCurrentTeam();
        try {
            int port = Integer.parseInt(
                kiponos.path("server", "http").get("port")
            );
            System.out.println("port=" + port + " mode=" + kiponos.getCurrentMode());
        } finally {
            kiponos.disconnect();
        }
    }
}
```

Gradle:

```gradle
implementation 'io.kiponos:sdk-boot-3:5.0.0.260710'
```

Tokens via env; profile via `-Dkiponos=...`.

---

## Proof in motion: the Kiponos Game

We did not ship 5.0 on faith alone.

**[Kiponos Game (Swinga)](https://github.com/Avdiel/kiponos-game)** is a libGDX 2D arena of bouncing shapes. The hero’s color, speed, pause, and more are **live Kiponos keys**. Change the dashboard; the arena obeys — same session, no restart.

It runs on **`io.kiponos:sdk-boot-3:5.0.0.260710`**. That is the dogfood.

```bash
git clone https://github.com/Avdiel/kiponos-game.git
cd kiponos-game
# local.properties or env: KIPONOS_ID / KIPONOS_ACCESS
./gradlew run
```

---

## Upgrade notes

| From | Action |
|------|--------|
| 4.4.x | Bump coordinate to `5.0.0.260710`; re-test disconnect paths |
| Custom hang-on-error wrappers | Consider deleting them — Offline/Safe are the product answer |
| “We catch Exception around get()” | Still fine; modes reduce how often you need heroics |

**Artifacts:**

- `io.kiponos:sdk-boot-3:5.0.0.260710`  
- `io.kiponos:sdk-boot-2:5.0.0.260710`  

Source & release notes: [github.com/Avdiel/kiponos-sdk](https://github.com/Avdiel/kiponos-sdk)  
Changelog: tag **`v5.0.0.260710`**

---

## What is still cooking (honest backlog)

5.0 is the reliability floor, not the ceiling:

- richer mode-change hooks for apps (`onRecovered`, …)  
- deeper Safe → Ready recovery policy  
- full live multi-mode matrix in CI  
- Boot2 parity where the facade is still thinner  

We ship majors when the contract changes. The contract changed.

---

## Try it

1. Create a free team on [kiponos.io](https://kiponos.io)  
2. Generate Connect tokens  
3. Drop in `sdk-boot-3:5.0.0.260710`  
4. Flip a value on the dashboard without redeploying  

Agent-friendly onboarding lives in **[kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)** (skills, golden Java, `AGENTS.md`).

---

### The one-liner

**Config files taught a generation of engineers to fear change.**  
**Kiponos 5.0 teaches the JVM to stay calm when the wire blips.**

Ship it. Bounce nothing.

---

*Moshe Avdiel · Kiponos.io · Java SDK 5.0.0.260710 on Maven Central*
