---
main_image: https://files.catbox.moe/dlkkmj.jpg
title: "Kiponos Java SDK 5.0 What’s New — Developer Guide"
published: false
tags: java, springboot, tutorial, devops
description: "Technical press-style guide: Ready/Offline/Safe modes, Last Known Good, Folder API, upgrade steps for 5.0.0.260710."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-sdk-5-whats-new-guide.md
---

# Kiponos Java SDK 5.0 What’s New — Developer Guide

This is the **technical companion** to the 5.0 milestone announcement: what changed, how modes behave, how to read config with the Folder API, and how to upgrade cleanly.

| | |
|--|--|
| **Version** | `5.0.0.260710` |
| **Maven group** | `io.kiponos` |
| **Artifacts** | `sdk-boot-3` (recommended), `sdk-boot-2` (legacy) |
| **Released** | 2026-07-12 (Maven Central) |

Happy product story: [SDK 5.0 milestone post](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-sdk-5-state-pattern.md).

---

## 1. Summary for busy engineers

**5.0 productizes client reliability** using a classic state pattern behind a stable facade:

| Mode | When | Config reads | Mutations / hooks | Notes |
|------|------|--------------|-------------------|--------|
| **Ready** | Connected to hub | Live in-memory tree | Full | Production happy path |
| **Offline** | Disconnected but LKG available | **Last Known Good** (read-only) | No-op / ignored | Survives hub blips without inventing values |
| **Safe** | Fail-closed | Empty / null-safe | No-op | Diagnostic dumps must **not** overwrite LKG |

Public entry remains:

```java
Kiponos kiponos = Kiponos.createForCurrentTeam();
```

You **do not** receive mode instances as the API surface. Modes switch internally. Query with:

```java
kiponos.getCurrentMode();
kiponos.isReadyMode();
kiponos.isOfflineMode();
kiponos.isSafeMode();
```

---

## 2. Install

### Gradle — Boot 3

```gradle
repositories { mavenCentral() }

dependencies {
    implementation 'io.kiponos:sdk-boot-3:5.0.0.260710'
}
```

### Gradle — Boot 2

```gradle
implementation 'io.kiponos:sdk-boot-2:5.0.0.260710'
```

### Runtime inputs

| Input | Mechanism |
|-------|-----------|
| Identity | env `KIPONOS_ID` |
| Access | env `KIPONOS_ACCESS` |
| Profile / tree slice | JVM `-Dkiponos="['App']['1.0.0']['dev']['base']"` |

Tokens and profile come from the Kiponos **Connect** screen for your team.

`sdk-common` is **not** a separate app dependency for consumers — boot jars include shared classes (fat-jar pattern).

---

## 3. Architecture (state pattern)

```text
Application code
      │
      ▼
Kiponos / KiponosBase     ◄── stable facade (one reference for app lifetime)
      │
      ▼
volatile SdkState
      ├── ReadyMode*     → live WebSocket + full Folder ops
      ├── OfflineMode*   → LKG reads only
      └── SafeMode*      → fail-closed + safe diagnostic dump
```

**Design rule:** never return Ready/Offline/Safe objects to callers.  
Returning a mode freezes the application on that mode forever.

Boot modules stay thin adapters (`ReadyModeBoot3`, connection controllers) over `sdk-common` cores.

---

## 4. Folder API (the surface you code against)

Root:

```java
KiponosFolder root = kiponos.getRootFolder();
```

### Navigation

```java
KiponosFolder server = root.path("server", "http");
// or stepwise:
KiponosFolder s = root.folder("server").folder("http");
```

Useful members (conceptual contract):

```text
path(String... folders)
folder(String folderName)
folderOrCreate(String folderName)
createFolder / renameFolder / deleteFolder / hasFolder
getFolderName / getFolders

get(String key)
get(String key, String def)
getInt(String key)
set(String key, String value)
hasKey(String keyName)
```

### Recommended reads

```java
// Prefer typed getters on the SDK surface
int port = kiponos.getRootFolder()
        .path("server", "http")
        .getInt("port");

String region = kiponos.getRootFolder()
        .path("deploy")
        .get("region", "eu-west-1");
```

**Avoid** `Integer.parseInt(folder.get("port"))` for normal integer keys — use **`getInt`**.

### Writes (Ready mode)

```java
kiponos.getRootFolder()
        .path("feature-flags")
        .set("checkout-v2", "true");
```

In Offline/safe modes, mutations are intentionally **no-ops** (do not assume silent remote write).

---

## 5. Last Known Good (LKG)

### Intent

When the hub is unreachable, apps should continue with a **known good snapshot**, not hang and not invent random defaults.

### Behavior sketch (5.0)

| Event | Behavior |
|-------|----------|
| Live tree healthy | Ready serves live data; LKG can be refreshed from good state |
| Disconnect / unreachable | Prefer **Offline** with LKG reads |
| No trustworthy backup | **Safe** fail-closed |
| Safe diagnostic dump | Writes **safe-dump-*** style artifacts — **never** overwrites LKG |

LKG resolution conceptually prefers: live memory → backup → on-disk LKG file under the SDK dump location.

### App guidance

- Treat Offline as **read-only truth of last good**  
- Do not assume writes succeed without Ready  
- Use mode queries if UI or probes must show “degraded but serving LKG”

---

## 6. Lifecycle

```java
Kiponos kiponos = Kiponos.createForCurrentTeam();
try {
    // work
} finally {
    kiponos.disconnect();
}
```

Notes:

- Always disconnect on orderly shutdown.  
- Intentional disconnect can remain in Offline rather than surprising flip back to Ready (verify against your mode transition policy).  
- Reconnect recovery continues to improve (see §10 open items).

---

## 7. Upgrade from 4.4.x

| Step | Action |
|------|--------|
| 1 | Change dependency version to `5.0.0.260710` |
| 2 | Rebuild / run tests against staging hub |
| 3 | Replace `parseInt(get(...))` with `getInt` where applicable |
| 4 | Re-test disconnect / reconnect paths (unit + one live smoke) |
| 5 | Optional: surface `getCurrentMode()` in health/admin endpoints |
| 6 | Confirm no code assumed “throw and hang” as the only offline strategy |

**Breaking risk:** low for pure read apps. Behavioral change is **improved** offline safety; re-validate anything that depended on exceptions during disconnect.

---

## 8. Example: Spring-friendly service skeleton

```java
import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.KiponosFolder;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy; // or jakarta.annotation.PreDestroy on Boot 3

@Component
public class CheckoutLimits {
    private final Kiponos kiponos;
    private final KiponosFolder limits;

    public CheckoutLimits() {
        this.kiponos = Kiponos.createForCurrentTeam();
        this.limits = kiponos.getRootFolder().path("checkout", "limits");
    }

    public int maxCartItems() {
        return limits.getInt("max-cart-items");
    }

    public String currency() {
        return limits.get("currency", "USD");
    }

    @PreDestroy
    public void shutdown() {
        kiponos.disconnect();
    }
}
```

Dashboard changes to `checkout/limits/*` propagate live while Ready; Offline continues serving LKG values.

---

## 9. Example config tree (dashboard)

```yaml
# Illustrative tree under your profile base
checkout:
  limits:
    max-cart-items: 50
    currency: USD
    soft-hold-seconds: 120
server:
  http:
    host: api.example.com
    port: 8080
feature-flags:
  checkout-v2: "true"
  dark-launch-percent: 15
```

Operators edit in the Kiponos web UI; apps read via Folder navigation — no redeploy.

---

## 10. Known follow-ups (honest roadmap)

These are **not** blockers for adopting 5.0:

| Item | Notes |
|------|--------|
| Mode-change hooks | e.g. `onRecovered`, `onSwitchToSafeMode` for app callbacks |
| Safe → Ready recovery polish | Explicit policy after fail-closed recovery |
| Full live multi-mode matrix in CI | Broader than unit Offline/Safe matrix |
| Boot2 modes parity | Boot3 is the recommended surface |

---

## 11. Verification checklist

- [ ] Dependency resolves from Maven Central  
- [ ] App starts with Connect tokens + profile  
- [ ] `getInt` / `path` reads match dashboard  
- [ ] Dashboard edit appears without restart (Ready)  
- [ ] Disconnect does not hang process (Offline or Safe)  
- [ ] `disconnect()` on shutdown  
- [ ] Optional: game demo on SDK 5.0 for stakeholder demo  

Live demo: [Kiponos Game](https://github.com/Avdiel/kiponos-game)  
Samples hub: [kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

## 12. Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)  
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)  
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)  
- Maven: [io.kiponos:sdk-boot-3](https://central.sonatype.com/artifact/io.kiponos/sdk-boot-3)  
- Milestone announcement: [devto-sdk-5-state-pattern.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-sdk-5-state-pattern.md)

---

**Bottom line for engineers:** bump to `5.0.0.260710`, keep using `getRootFolder().path(...).getInt(...)`, and treat modes + LKG as the supported reliability model — not an optional cache you reinvent.
