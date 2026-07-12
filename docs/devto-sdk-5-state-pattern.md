---
title: "Kiponos Java SDK 5.0 Is Here — Live Config, Calmer Apps, Bigger Smile"
published: false
tags: java, springboot, opensource, devops
description: "Proud milestone: SDK 5.0 on Maven Central with Ready, Offline, and Safe modes plus Last Known Good. Happy upgrade, real resilience."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-sdk-5-state-pattern.md
main_image: https://files.catbox.moe/lbowqg.jpg
---

Some product moments feel like maintenance.  
**This one feels like a celebration.**

Today we are proud to ship **Kiponos Java SDK 5.0** — the release that takes everything teams loved about live dashboard config and makes the client as confident as the product story.

No restart theater.  
No “hope the socket holds.”  
Just a calmer, happier way to run apps that stay in sync with the people operating them.

## Maven Central — available now

```gradle
repositories { mavenCentral() }

dependencies {
    // Spring Boot 3 / Jakarta (recommended)
    implementation 'io.kiponos:sdk-boot-3:5.0.0.260710'

    // Spring Boot 2 / javax (legacy)
    // implementation 'io.kiponos:sdk-boot-2:5.0.0.260710'
}
```

**Version:** `5.0.0.260710`  
**Group:** `io.kiponos`  
**Artifacts:** `sdk-boot-3` · `sdk-boot-2`

---

## Why this is a happy milestone

[Kiponos.io](https://kiponos.io) has always been about **joy of control**: open the dashboard, change a value, and running apps feel it immediately over WebSocket — local reads on the hot path, zero redeploy ritual.

With **5.0**, that joy gets a reliability backbone teams can trust in production:

| Mode | How it feels |
|------|----------------|
| **Ready** | Connected, live tree, full power — the experience you demo and ship |
| **Offline** | Network blip? Keep reading **Last Known Good** config calmly |
| **Safe** | Nothing trustworthy yet? Fail closed, stay empty-safe, protect LKG |

You still hold **one** object forever: `Kiponos`.  
Modes switch *inside* the facade. Your code stays simple. Your ops stay human.

That design choice is intentional — and a little proud. We learned (the hard way, on an earlier branch) that handing mode objects to apps freezes them on one personality forever. **5.0 keeps the facade stable and the resilience internal.**

---

## What you gain in one picture

```text
Your app
  └── Kiponos.createForCurrentTeam()
        └── Ready  ·  Offline (LKG)  ·  Safe
```

- Dashboard edits still flow as **live deltas**  
- Hot path still does **local** `get` / `getInt` / `path(...)`  
- Disconnects become **modes**, not meltdowns  
- LKG is a first-class product feature, not a weekend hack

---

## The API you actually write (typed, clean)

```java
import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.KiponosFolder;

public class Demo {
    public static void main(String[] args) {
        Kiponos kiponos = Kiponos.createForCurrentTeam();
        try {
            KiponosFolder http = kiponos.getRootFolder()
                    .path("server", "http");

            int port = http.getInt("port");
            String host = http.get("host", "localhost");

            System.out.println(host + ":" + port);
            System.out.println("mode=" + kiponos.getCurrentMode());
        } finally {
            kiponos.disconnect();
        }
    }
}
```

`KiponosFolder` is the navigation surface you already know:

- `path(...)` / `folder(...)` / `folderOrCreate(...)`  
- `get` / `getInt` / `set` / `hasKey`  
- folder CRUD when you need it  

No string-to-int gymnastics required for integers — **`getInt` is the happy path.**

---

## Feel it in five minutes (pick your joy)

### 1. Golden Java (minimal connect)

Open-source onboarding kit:  
[github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io) → `golden/java`

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/golden/java
# Connect tokens + profile from your Kiponos account
./gradlew run
```

### 2. Kiponos Game (visceral demo)

A libGDX arena of shapes controlled **live** from the dashboard — color, speed, pause, more:

**[github.com/Avdiel/kiponos-game](https://github.com/Avdiel/kiponos-game)**  
(Already on **SDK 5.0**. Public samples will also live under the **kiponos-io** account as we grow the showcase.)

Change a value in the hub. Watch the world respond. That is the product, not a slide.

### 3. Agent skills

If you build with AI agents, the public repo ships an installable skill + `AGENTS.md` so integration is machine-readable, not folklore.

---

## For teams already on 4.x

Upgrade is mostly **additive confidence**:

1. Bump to `5.0.0.260710`  
2. Keep `KIPONOS_ID` / `KIPONOS_ACCESS` and `-Dkiponos="['app']['…']['…']['…']"`  
3. Prefer `getInt` / folder navigation over ad-hoc parsing  
4. Optionally branch ops UI on `isReadyMode()` / `isOfflineMode()` / `isSafeMode()`  
5. Call `disconnect()` on shutdown (same good citizenship as always)

Full technical detail lives in the companion guide:  
**[SDK 5.0 What’s New — Developer Guide](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-sdk-5-whats-new-guide.md)**

---

## What this says about where we’re going

5.0 is a **major** because the client contract matured:

- resilience is **productized**, not “reconnect harder”  
- LKG is **real**  
- modes are **observable**  
- Boot 3 and Boot 2 ship together on Central  

We’re still building (mode hooks, deeper recovery polish, richer demos).  
But the floor just rose — and that is worth a smile.

---

## Thank you

To everyone who signed up, clicked around, or integrated Kiponos quietly: **this release is for you.**  
Live config should feel light in the demo and steady in production. **5.0 is our happy step toward both.**

**Ship the dependency. Keep the dashboard open. Enjoy the loop.**

---

### Links

| | |
|--|--|
| Product | [kiponos.io](https://kiponos.io) |
| Maven (boot-3) | [central.sonatype.com/artifact/io.kiponos/sdk-boot-3](https://central.sonatype.com/artifact/io.kiponos/sdk-boot-3) |
| Public docs & samples | [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io) |
| Live game demo | [github.com/Avdiel/kiponos-game](https://github.com/Avdiel/kiponos-game) |
| Technical What’s New | [devto-sdk-5-whats-new-guide.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-sdk-5-whats-new-guide.md) |

*Kiponos Java SDK 5.0.0.260710 — with pride.*
