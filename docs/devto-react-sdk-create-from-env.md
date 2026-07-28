---
main_image: https://files.catbox.moe/dlkkmj.jpg
title: "Your React SPA Is Not a Node Process — Why Kiponos createFromEnv Lives on the Server"
published: true
tags: java, architecture, devops, kiponos
description: "Kiponos still ships Java and Python SDKs. For SaaS UIs we added a Node server participant with createFromEnv — same hub, no Connect tokens in the browser bundle."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-react-sdk-create-from-env.md
---

**The Aha:** A “React app” is not automatically a process with secrets. If the browser runs your UI, **Connect tokens must not live in the bundle** — the hub participant is the **server process** behind it (same moral as Java `createForCurrentTeam`).

Kiponos product SDKs remain **Java (Spring Boot 2/3) and Python**. What we added is a **Node server participant** for SaaS backends that already front a UI — not a free-for-all browser token dump.

## The lie that wastes a week

Teams say: “We’ll put `KIPONOS_ID` in `.env` for the web app.”

On a **server**, `.env` is process environment — exactly how Java and Python already load Connect tokens.  
On a **bundled SPA**, `VITE_*` / `REACT_APP_*` is **compiled into public JavaScript**.

Same filename. Opposite threat model.

| Runtime | What “env” means |
|---------|------------------|
| JVM / Python / Node **process** | Real environment (like `createForCurrentTeam`) |
| Bundled SPA in the visitor’s browser | Strings every visitor can download |

## What we shipped

**`@kiponos/react`** (Node entry: `@kiponos/react/server`) — real-time Kiponos **server** participant:

```js
import { Kiponos } from '@kiponos/react/server';

// Java parity: identity from process env only
const kip = Kiponos.createFromEnv(); // or createForCurrentTeam()
await kip.connect();
await kip.path('family', 'mirror-phone').set('status-moshe', 'online');
```

- Reads **`KIPONOS_ID` / `KIPONOS_ACCESS` / `KIPONOS`** only from process env  
- **Rejects** constructing a client with raw tokens in application code  
- Same hub as the **Java SDK**, **Python SDK**, and the dashboard  

Java still looks like this:

```java
Kiponos k = Kiponos.createForCurrentTeam();
String status = k.path("family", "mirror-phone").get("status-moshe", "—");
```

One tree. Four participant *shapes*: dashboard human, JVM, Python, Node server.

### Browser UI pattern (SaaS)

```text
SPA in the browser  ↔  your Node API (createFromEnv + SSE)  ↔  Kiponos hub
                              ↑
                     same env contract as Java
```

The SPA never holds Connect tokens. It posts intent to **your** backend; the backend is the hub peer.

```
┌─────────────┐     set/get      ┌──────────────┐
│  Dashboard  │◄────────────────►│              │
└─────────────┘                  │              │
┌─────────────┐     WebSocket    │  Kiponos hub │
│  Java SDK   │◄────────────────►│  (one tree)  │
└─────────────┘                  │              │
┌─────────────┐                  │              │
│ Python SDK  │◄────────────────►│              │
└─────────────┘                  │              │
┌─────────────┐     createFromEnv│              │
│ Node server │◄────────────────►│              │
└──────┬──────┘                  └──────────────┘
       │ SSE / API only
       ▼
┌─────────────┐
│  SPA (UI)   │  ← no Connect tokens
└─────────────┘
```

## Live demo on kiponos.io

**[Mirror Phone](https://kiponos.io/mirror/)** — family status wall:

- Node process: `createFromEnv` + team topics  
- SPA UI: `EventSource` only  
- Tree: `family/mirror-phone/*`  
- Linked from the **[Operator](https://kiponos.io/operator/)** home grid  

Change a status on the wall; Java or Python on the same profile can `get()` the new value without a redeploy. That is the product: **state is the protocol**.

## Hub tree

```text
family/mirror-phone/status-moshe = online|away|focus
family/mirror-phone/status-mush  = shift|home|—
family/mirror-phone/note         = free text
family/mirror-phone/last-ping    = ISO timestamp
family/mirror-phone/mood         = focused|calm|…
```

Local `get()` on the hot path. Dashboard or any SDK `set()` when the world changes.

## Why not browser WebSocket with Connect tokens?

Browsers cannot attach custom HTTP headers on the WebSocket upgrade the way the Java client and Node `ws` do. The **web dashboard** uses a **login session cookie** on a different endpoint. Connect tokens are **app/service identity** — keep them on the process that “is” your SaaS backend, the same place you already put secrets for Spring.

We did **not** weaken the SDK handshake “for CORS.” CORS is not the issue; **secret placement** is.

## What stays versioned vs live

| Jar / repo (versioned) | Hub (live) |
|------------------------|------------|
| Code paths & clamps | Operational status fields |
| UI layout & validation | Human judgment under pressure |
| Fail-closed defaults | Temporary incident overrides |
| Schema of keys | Current values |

## Clone and run the pattern

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
# Java examples: examples/java/*
# Profile: ['app']['release']['env']['config']
# Node peer: createFromEnv with KIPONOS_ID / KIPONOS_ACCESS / KIPONOS
```

```js
import { Kiponos } from '@kiponos/react/server'
const kip = Kiponos.createForCurrentTeam()
await kip.connect()
// set/get path — same tree Java already uses
```

- Getting started: [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/GETTING-STARTED.md)  
- Product: [kiponos.io](https://kiponos.io)  
- Live: [Mirror Phone](https://kiponos.io/mirror/)  
- Public tree: [kiponos-io on GitHub](https://github.com/kiponos-io/kiponos-io)

## The moral

**People should not have to ship a release to make a decision** — and they should not paste service tokens into a SPA to get there.

Java and Python remain the product SDKs. `createFromEnv` on Node is the same contract those already had: **identity is where the process runs.** The UI can stay thin, live, and honest.
