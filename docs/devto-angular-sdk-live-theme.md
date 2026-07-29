---
title: "Angular + Kiponos: Live Theme Toggle Without a SPA Redeploy"
published: false
tags: angular, typescript, devops, websocket, kiponos
description: Use the Kiponos Angular SDK to live-control theme string on the hub (`ui/theme`) without redeploy. createFromEnv, Java parity, and a runnable monorepo path.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-angular-sdk-live-theme.md
---

# Angular + Kiponos: Live Theme Toggle Without a SPA Redeploy

**Use case.** Control **theme string on the hub** at hub path `ui/theme` from a **@kiponos/angular** process peer — while Java and the dashboard stay in sync.

I have watched a design lead wait for a frontend deploy to flip dark mode for a demo. The fix is not another SPA build. It is a hub key.

## Package

https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-angular-sdk

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

1. Export `KIPONOS_ID`, `KIPONOS_ACCESS`, `KIPONOS` (Connect UI).  
2. Build the SDK package under `sdks/kiponos-angular-sdk`.  
3. Set `ui/theme` from Node; read it from Java `createForCurrentTeam()`.  
4. Optional: open the dashboard on the same profile.

## The moral

People should not have to ship a release to make a decision. Live **theme string on the hub** belongs on the hub — and the Angular SDK is a peer on that hub.

— Kiponos · https://kiponos.io
