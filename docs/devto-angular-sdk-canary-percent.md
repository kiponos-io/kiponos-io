---
title: "Angular + Kiponos: Canary Percent Live on the Hub"
published: false
tags: angular, typescript, devops, websocket, kiponos
description: Use the Kiponos Angular SDK to live-control traffic split percent (`release/canary-percent`) without redeploy. createFromEnv, Java parity, and a runnable monorepo path.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-angular-sdk-canary-percent.md
---

# Angular + Kiponos: Canary Percent Live on the Hub

**Use case.** Control **traffic split percent** at hub path `release/canary-percent` from a **@kiponos/angular** process peer — while Java and the dashboard stay in sync.

I have heard an on-call engineer say they needed five percent of traffic, not a full release train. The fix is not another SPA build. It is a hub key.

## Package

https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-angular-sdk

Companion: `examples/java/angular-sdk-canary-percent`

## Code

```ts
import { Kiponos, injectKiponos, provideKiponos } from '@kiponos/angular';

// Node BFF
const client = Kiponos.createFromEnv();
await client.connect();
await client.path('release').set('canary-percent', '5');

// Angular component (client injected via provideKiponos)
// readonly v = injectKiponos().value('release/canary-percent', { defaultValue: '5' });
```

## How to try

1. Export `KIPONOS_ID`, `KIPONOS_ACCESS`, `KIPONOS` (Connect UI).  
2. Build the SDK package under `sdks/kiponos-angular-sdk`.  
3. Set `release/canary-percent` from Node; read it from Java `createForCurrentTeam()`.  
4. Optional: open the dashboard on the same profile.

## The moral

People should not have to ship a release to make a decision. Live **traffic split percent** belongs on the hub — and the Angular SDK is a peer on that hub.

— Kiponos · https://kiponos.io
