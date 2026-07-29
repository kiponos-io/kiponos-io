---
title: "Angular + Kiponos: API Rate Limit Knobs Without Bounce"
published: false
tags: angular, typescript, devops, websocket, kiponos
description: Use the Kiponos Angular SDK to live-control RPS cap (`api/rate-limit-rps`) without redeploy. createFromEnv, Java parity, and a runnable monorepo path.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-angular-sdk-rate-limit.md
---

# Angular + Kiponos: API Rate Limit Knobs Without Bounce

**Use case.** Control **RPS cap** at hub path `api/rate-limit-rps` from a **@kiponos/angular** process peer — while Java and the dashboard stay in sync.

I have met teams that bounced pods just to change a rate limit integer. The fix is not another SPA build. It is a hub key.

## Package

https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-angular-sdk

Companion: `examples/java/angular-sdk-rate-limit`

## Code

```ts
import { Kiponos, injectKiponos, provideKiponos } from '@kiponos/angular';

// Node BFF
const client = Kiponos.createFromEnv();
await client.connect();
await client.path('api').set('rate-limit-rps', '100');

// Angular component (client injected via provideKiponos)
// readonly v = injectKiponos().value('api/rate-limit-rps', { defaultValue: '100' });
```

## How to try

1. Export `KIPONOS_ID`, `KIPONOS_ACCESS`, `KIPONOS` (Connect UI).  
2. Build the SDK package under `sdks/kiponos-angular-sdk`.  
3. Set `api/rate-limit-rps` from Node; read it from Java `createForCurrentTeam()`.  
4. Optional: open the dashboard on the same profile.

## The moral

People should not have to ship a release to make a decision. Live **RPS cap** belongs on the hub — and the Angular SDK is a peer on that hub.

— Kiponos · https://kiponos.io
