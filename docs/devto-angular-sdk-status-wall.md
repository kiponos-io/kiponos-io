---
main_image: https://files.catbox.moe/x854ey.jpg
title: "Angular + Kiponos: Shared Status Wall Across Peers"
published: false
tags: angular, typescript, devops, websocket, kiponos
description: Use the Kiponos Angular SDK to live-control ops status leaf (`demo/status-wall/alpha`) without redeploy. createFromEnv, Java parity, and a runnable Install with `npm install @kiponos/angular`. Runnable example included.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-angular-sdk-status-wall.md
---

# Angular + Kiponos: Shared Status Wall Across Peers

**Use case.** Control **ops status leaf** at hub path `demo/status-wall/alpha` from a **@kiponos/angular** process peer — while Java and the dashboard stay in sync.

I have always preferred a wall the whole team can see over a chat that scrolls away. The fix is not another SPA build. It is a hub key.

## Package

```bash
npm install @kiponos/angular
```

https://www.npmjs.com/package/@kiponos/angular  
Source: https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-angular-sdk

Companion: `examples/java/angular-sdk-status-wall`

## Code

```ts
import { Kiponos, injectKiponos, provideKiponos } from '@kiponos/angular';

// Node BFF
const client = Kiponos.createFromEnv();
await client.connect();
await client.path('demo', 'status-wall').set('alpha', 'idle');

// Angular component (client injected via provideKiponos)
// readonly v = injectKiponos().value('demo/status-wall/alpha', { defaultValue: 'idle' });
```

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

## The moral

People should not have to ship a release to make a decision. Live **ops status leaf** belongs on the hub — and the Angular SDK is a peer on that hub.

— Kiponos · https://kiponos.io
