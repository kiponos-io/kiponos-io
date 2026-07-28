---
title: "Your React SPA Is Not a Node Process — Why Kiponos createFromEnv Lives on the Server"
published: false
tags: javascript, node, react, architecture
description: "We shipped a React/Node SDK for Kiponos with Java-style createFromEnv. The bi-directional hub is real — but Connect tokens do not belong in a bundled SPA."
---

**The Aha:** A “React app” is not automatically a process with secrets. If the browser runs your UI, **Connect tokens must not live in the bundle** — the hub participant is the **Node (or JVM) process** behind it.

## The lie that wastes a week

Teams say: “We’ll put `KIPONOS_ID` in `.env` for the React app.”

On a **server**, `.env` is process environment.  
On a **Vite/CRA SPA**, `VITE_*` / `REACT_APP_*` is **compiled into public JavaScript**.

Same filename. Opposite threat model.

| Runtime | What “env” means |
|---------|------------------|
| Node / Spring | Real process environment (like Java `createForCurrentTeam`) |
| Bundled SPA | Strings baked into what every visitor can download |

## What we shipped

**`@kiponos/react`** — real-time Kiponos participant for **Node/server-first** apps:

```js
import { Kiponos } from '@kiponos/react/server';

const kip = Kiponos.createFromEnv(); // or createForCurrentTeam()
await kip.connect();
await kip.path('family', 'mirror-phone').set('status-moshe', 'online');
```

- Reads **`KIPONOS_ID` / `KIPONOS_ACCESS` / `KIPONOS`** only from process env  
- **Rejects** `new KiponosClient({ idToken, accessToken })`  
- Same hub as **Java SDK**, **Python**, and the dashboard  

### Browser UI pattern (SaaS)

```text
React SPA  ↔  your Node API (createFromEnv + SSE)  ↔  Kiponos hub
```

The SPA never holds Connect tokens. It posts intent to **your** backend; the backend is the hub peer.

## Live demo on kiponos.io

**[Mirror Phone](https://kiponos.io/mirror/)** — family status wall:

- Node process: `createFromEnv` + team topics  
- React UI: `EventSource` only  
- Tree: `family/mirror-phone/*`  
- Linked from the **[Operator](https://kiponos.io/operator/)** home grid  

Change a status on the phone wall; other participants on the same profile see the delta. That is the product: **state is the protocol**.

## Architecture

```
Dashboard / Java / Python / Node (createFromEnv)
        │  WebSocket deltas
        ▼
   Kiponos hub (one tree)
        │
        ▼
   Your API ──SSE──► React SPA (no secrets)
```

## Why not browser WebSocket with Connect tokens?

Browsers cannot attach custom HTTP headers on the WebSocket upgrade the way Java/`ws` do. The dashboard uses a **login session cookie** on a different endpoint. Connect tokens are **app/service identity** — keep them on the process that “is” your SaaS backend.

## Clone the idea

```bash
# SDK (env-only)
# KIPONOS_ID / KIPONOS_ACCESS / KIPONOS=...
import { Kiponos } from '@kiponos/react/server'
const kip = Kiponos.createForCurrentTeam()
await kip.connect()
```

- Product: [kiponos.io](https://kiponos.io)  
- Live: [Mirror Phone](https://kiponos.io/mirror/)  
- Java SDK: still `Kiponos.createForCurrentTeam()` — same hub, fourth participant type is now **Node/React backends**

## The moral

**People should not have to ship a release to change shared operational state** — and they should not paste service tokens into a SPA to get there. `createFromEnv` is the same contract Java already had: **identity is where the process runs.**
