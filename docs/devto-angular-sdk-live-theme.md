---
main_image: https://files.catbox.moe/z65ex6.jpg
title: "Angular + Kiponos: Live Theme Toggle Without a SPA Redeploy"
published: false
tags: angular, typescript, devops, websocket, kiponos
description: Use the Kiponos Angular SDK to live-control theme string on the hub (`ui/theme`) without redeploy. createFromEnv, Java parity, and a runnable Install with `npm install @kiponos/angular`. Runnable example included.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-angular-sdk-live-theme.md
---

# Angular + Kiponos: Live Theme Toggle Without a SPA Redeploy

**Use case.** Control **theme string on the hub** at hub path `ui/theme` from a **@kiponos/angular** process peer — while Java and the dashboard stay in sync.

I have watched a design lead wait for a frontend deploy to flip dark mode for a demo. The fix is not another SPA build. It is a hub key.

## Package

```bash
npm install @kiponos/angular
```

https://www.npmjs.com/package/@kiponos/angular  
Source: https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-angular-sdk

Companion: `examples/java/angular-sdk-live-theme`

## Code

```ts
import { Kiponos, injectKiponos, provideKiponos } from '@kiponos/angular';

// Node BFF
const client = Kiponos.createFromEnv();
await client.connect();
await client.path('ui').set('theme', 'dark');

// Angular component (client injected via provideKiponos)
// readonly v = injectKiponos().value('ui/theme', { defaultValue: 'dark' });
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

People should not have to ship a release to make a decision. Live **theme string on the hub** belongs on the hub — and the Angular SDK is a peer on that hub.

— Kiponos · https://kiponos.io
