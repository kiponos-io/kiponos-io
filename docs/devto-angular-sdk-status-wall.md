---
main_image: https://files.catbox.moe/x854ey.jpg
title: "Angular + Kiponos: Shared Status Wall Across Peers"
published: false
tags: angular, typescript, devops, kiponos
description: "Live shared status wall fields on the Kiponos hub from a Angular server peer — createFromEnv, no SPA secrets, Java parity. Install @kiponos/angular."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-angular-sdk-status-wall.md
---

**The Aha:** Shared status wall fields is a judgment call under pressure. If the only path is a SPA rebuild, you do not have a control plane — you have a ceremony.

I have sat through a room that looked like this: dashboard, JVM, and Angular API disagreeing until the next deploy. Someone said “we just need to flip it.” Someone else opened a PR. CI ran. The window closed.

The fix is not another frontend deploy. It is a **hub key** read by a **process** that is allowed to hold Connect tokens.

## What went wrong (the human version)

A SaaS “Angular app” is usually two things mashed into one word:

| Piece | Runs where | Holds Connect tokens? |
|-------|------------|------------------------|
| SPA / browser bundle | Visitor’s machine | **Never** |
| Node (or any) API | Your cluster | **Yes — like Java** |

`.env` on a build host is not “safe” if the bundler inlines `VITE_*` / `NG_*` into public JS. The **Node/Angular server process** is the participant. The UI talks to **your** API (SSE/REST) — not Connect tokens to the hub.

So the Super Pattern for frontends is dull on purpose:

> Live hub + process identity + thin UI mirror = decisions that move without a release **or** a leaked token.

## Package

```bash
npm install @kiponos/angular
```

- npm: https://www.npmjs.com/package/@kiponos/angular
- Source: https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-angular-sdk
- Companion Java tree: `examples/java/angular-sdk-status-wall` (same hub moral)

## Hub path (name it so humans find it at 3am)

```text
demo/status-wall
```

Ops or another SDK client **sets** the leaf. Every peer **gets** locally after WebSocket deltas. No “which replica still has the old YAML?” scavenger hunt.

## Code (server peer — process env only)

```ts
import { Kiponos } from '@kiponos/angular/server';

const kip = Kiponos.createFromEnv(); // or createForCurrentTeam()
await kip.connect();
await kip.path('demo', 'status-wall').set('status-alpha', 'focus');
await kip.path('demo', 'status-wall').set('last-ping', new Date().toISOString());
// Browser UI: call *your* API / SSE — never put KIPONOS_ACCESS in the SPA bundle
```

Java still peers on the same tree:

```java
Kiponos k = Kiponos.createForCurrentTeam();
String alpha = k.path("demo", "status-wall").get("status-alpha", "—");
// Node/Angular wrote this seconds ago — no redeploy
```

That is the whole product brief: **structure in the jar, selection on the hub**.

## Browser UI pattern (keep the SPA thin)

```text
SPA / Angular UI  ↔  your API (@kiponos/angular createFromEnv + SSE)  ↔  Kiponos hub
                              ↑
                     same env contract as Java
```

| Do | Don’t |
|----|--------|
| `createFromEnv` / `createForCurrentTeam` on the server | Token constructors in client bundles |
| Bridge browsers with API/SSE you control | Expose Connect tokens for “simpler WebSocket” |
| Name hub paths for war rooms | Hide knobs in undocumented env files |
| Measure seconds from judgment to effect | Count deploys as progress |

## How to try

```bash
export KIPONOS_ID=… KIPONOS_ACCESS=…
export KIPONOS="['MyApp']['1.0']['Dev']['base']"

npm install @kiponos/angular

git clone https://github.com/kiponos-io/kiponos-io.git
# Node status-wall peer (writes live leaves):
cd kiponos-io/examples/node/react-status-wall
npm install && npm start

# Optional Java peer on the same hub profile:
cd ../../java/react-sdk-hub-peer
# export same KIPONOS_* then ./gradlew test run when wired like sibling examples
```

Flip `status-alpha` (or your domain leaf) on the dashboard or from the Node peer. The Java print should follow the hub — not the last SPA deploy.

Full public surface: [kiponos.io](https://kiponos.io) · [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

## Old world vs live hub

| Move | Old world (SPA as participant) | Live hub (Node/Angular server process) |
|------|--------------------------------|------------------------|
| Change shared status wall fields | Ship SPA / service build | Dashboard or SDK `set()` |
| Where tokens live | Often public JS | Process env only |
| Java sees the change | After redeploy folklore | Same tree, live deltas |
| Rollback | Redeploy previous artifact | Flip the value back |

## War-room protocol (keep this boring)

1. Name the hub path out loud: `demo/status-wall`  
2. Speak the clamp / allowlist before anyone types  
3. Write reason code with the change (`demo`, `incident`, `peak`)  
4. Watch the metric that matters for five minutes  
5. Revert or step — never leave a “temporary” value as silent default  
6. Postmortem line: who moved the key, from→to, whether automation should own the next flip  

## The moral

**People should not have to ship a release to make a decision** — and they should not paste service tokens into a SPA to share state.

Identity is **where the process runs**. Angular’s job is to be a first-class peer on the hub, not a bundler that smuggles secrets. Ship the peer once. Leave the bundle alone when the only thing that changed is **shared status wall fields**.

— Kiponos · https://kiponos.io
