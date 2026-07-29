---
main_image: https://litter.catbox.moe/2l7x1l.jpg
title: "We Opened the Kiponos Hub to Angular — Live Config Without Redeploy (Announcement)"
published: true
tags: angular, typescript, devops, websocket, opensource
description: Happy professional announcement — @kiponos/angular is on npm. Install with npm install @kiponos/angular. Java-parity createFromEnv, Angular Signals, BFF-safe design, runnable example.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-angular-sdk-announcement.md
---

# We Opened the Kiponos Hub to Angular — Live Config Without Redeploy

**Announcement.** Today we are happy to put a first-class **Angular** participant on the same Kiponos real-time config hub that Java, Python, and React already share.

If you have been waiting for “live knobs in Angular without shipping a SPA build,” this is that day.

## Why this matters

Kiponos is a **real-time config hub**: nested keys, WebSocket/STOMP deltas, dashboard peers. Java teams have used `Kiponos.createForCurrentTeam()` for years. Agents use Python. React/Node just joined with `createFromEnv`.

Angular teams deserved the same contract:

| Capability | Java | Angular (`@kiponos/angular`) |
|------------|------|------------------------------|
| Identity | process env | `createFromEnv` / `createForCurrentTeam` |
| Navigate | `path("a","b")` | same |
| Get / set | local get, live set | same + Promises |
| Live UI | listeners | **Signals** + `after*` hooks |
| Tokens in browser | never | **never** (BFF injects client) |

## Install (npm)

```bash
npm install @kiponos/angular
```

Public package: https://www.npmjs.com/package/@kiponos/angular  
Runnable Node example: `examples/node/angular-status-wall`  
Source: https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-angular-sdk

## 60-second Node start

```ts
import { Kiponos } from '@kiponos/angular/server';

const kip = Kiponos.createFromEnv();
await kip.connect();
await kip.ensurePath('ui');
await kip.path('ui').set('theme', 'dark');
console.log(kip.get('theme', undefined, 'ui'));
```

Env (same as Java):

```bash
export KIPONOS_ID=…          # Connect UI
export KIPONOS_ACCESS=…
export KIPONOS="['MyApp']['1.0']['Dev']['base']"
```

## Angular DI + Signals

```ts
// app.config.ts — inject a server-created client (SSR/BFF)
import { provideKiponos } from '@kiponos/angular';
providers: [provideKiponos({ client })];

// component
import { injectKiponos } from '@kiponos/angular';

export class ThemeBadge {
  readonly kip = injectKiponos();
  readonly theme = this.kip.value('ui/theme', { defaultValue: 'dark' });
}
```

Browser SPAs **must not** embed Connect tokens. Pattern:

```text
Angular SPA  ↔  your Node API (createFromEnv)  ↔  Kiponos hub
```

## Proof across languages

Companion example: `examples/java/angular-sdk-hub-peer`  
A Java process reads the same `demo/status-wall/*` keys your Angular BFF writes. Dashboard and Python agents can sit on the same profile. One tree. Many voices.

## What ships in v0.1

- ReadyMode honesty (online hub peer)  
- STOMP heartbeats (long-lived connections)  
- `path` / `folderOrCreate` / `ensurePath`  
- `afterValueUpdated`, `afterKeyCreated`, …  
- Live E2E against production hub (36 tests)  

Out of v0.1: Offline/LKG modes (Java has them; Angular will follow).

## How to try

```bash
# 1) Install the SDK from npm
npm install @kiponos/angular

# 2) Connect credentials (same as Java)
export KIPONOS_ID=… KIPONOS_ACCESS=…
export KIPONOS="['MyApp']['1.0']['Dev']['base']"

# 3) Runnable Node peer (BFF process)
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/node/angular-status-wall
npm install
npm start
# → writes demo/status-wall/* on the hub

# 4) Optional Java peer (same tree)
cd ../../java/angular-sdk-hub-peer
```

Package: [@kiponos/angular on npm](https://www.npmjs.com/package/@kiponos/angular)

## Closing

We are genuinely happy about this. Real-time config should not be a JVM-only club. Angular is now a peer on the hub — with Signals for the template and process identity for secrets.

**Ship decisions, not SPA releases.**

— Kiponos team · https://kiponos.io
