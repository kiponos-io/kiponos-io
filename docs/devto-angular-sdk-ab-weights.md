---
title: "Angular + Kiponos: A/B Checkout Weights in Real Time"
published: false
tags: angular, typescript, devops, websocket, kiponos
description: Use the Kiponos Angular SDK to live-control experiment weight (`checkout/ab-weight-b`) without redeploy. createFromEnv, Java parity, and a runnable monorepo path.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-angular-sdk-ab-weights.md
---

# Angular + Kiponos: A/B Checkout Weights in Real Time

**Use case.** Control **experiment weight** at hub path `checkout/ab-weight-b` from a **@kiponos/angular** process peer — while Java and the dashboard stay in sync.

I have watched product wait overnight for an A/B weight change that should have been a hub key. The fix is not another SPA build. It is a hub key.

## Package

https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-angular-sdk

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

1. Export `KIPONOS_ID`, `KIPONOS_ACCESS`, `KIPONOS` (Connect UI).  
2. Build the SDK package under `sdks/kiponos-angular-sdk`.  
3. Set `checkout/ab-weight-b` from Node; read it from Java `createForCurrentTeam()`.  
4. Optional: open the dashboard on the same profile.

## The moral

People should not have to ship a release to make a decision. Live **experiment weight** belongs on the hub — and the Angular SDK is a peer on that hub.

— Kiponos · https://kiponos.io
