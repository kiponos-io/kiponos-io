---
main_image: https://files.catbox.moe/68lli5.jpg
title: "Angular + Kiponos: A/B Checkout Weights in Real Time"
published: false
tags: angular, typescript, devops, websocket, kiponos
description: Use the Kiponos Angular SDK to live-control experiment weight (`checkout/ab-weight-b`) without redeploy. createFromEnv, Java parity, and a runnable Install with `npm install @kiponos/angular`. Runnable example included.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-angular-sdk-ab-weights.md
---

# Angular + Kiponos: A/B Checkout Weights in Real Time

**Use case.** Control **experiment weight** at hub path `checkout/ab-weight-b` from a **@kiponos/angular** process peer — while Java and the dashboard stay in sync.

I have watched product wait overnight for an A/B weight change that should have been a hub key. The fix is not another SPA build. It is a hub key.

## Package

```bash
npm install @kiponos/angular
```

https://www.npmjs.com/package/@kiponos/angular  
Source: https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-angular-sdk

Companion: `examples/java/angular-sdk-ab-weights`

## Code

```ts
import { Kiponos, injectKiponos, provideKiponos } from '@kiponos/angular';

// Node BFF
const client = Kiponos.createFromEnv();
await client.connect();
await client.path('checkout').set('ab-weight-b', '50');

// Angular component (client injected via provideKiponos)
// readonly v = injectKiponos().value('checkout/ab-weight-b', { defaultValue: '50' });
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

People should not have to ship a release to make a decision. Live **experiment weight** belongs on the hub — and the Angular SDK is a peer on that hub.

— Kiponos · https://kiponos.io
