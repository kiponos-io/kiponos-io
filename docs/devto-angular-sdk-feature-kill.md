---
title: "Angular + Kiponos: Feature Kill Switch From the Hub"
published: false
tags: angular, typescript, devops, websocket, kiponos
description: Use the Kiponos Angular SDK to live-control boolean-ish kill flag (`flags/feature-x`) without redeploy. createFromEnv, Java parity, and a runnable monorepo path.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-angular-sdk-feature-kill.md
---

# Angular + Kiponos: Feature Kill Switch From the Hub

**Use case.** Control **boolean-ish kill flag** at hub path `flags/feature-x` from a **@kiponos/angular** process peer — while Java and the dashboard stay in sync.

I have sat through a war room where the only fix was a redeploy to turn a feature off. The fix is not another SPA build. It is a hub key.

## Package

https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-angular-sdk

Companion: `examples/java/angular-sdk-feature-kill`

## Code

```ts
import { Kiponos, injectKiponos, provideKiponos } from '@kiponos/angular';

// Node BFF
const client = Kiponos.createFromEnv();
await client.connect();
await client.path('flags').set('feature-x', 'off');

// Angular component (client injected via provideKiponos)
// readonly v = injectKiponos().value('flags/feature-x', { defaultValue: 'off' });
```

## How to try

1. Export `KIPONOS_ID`, `KIPONOS_ACCESS`, `KIPONOS` (Connect UI).  
2. Build the SDK package under `sdks/kiponos-angular-sdk`.  
3. Set `flags/feature-x` from Node; read it from Java `createForCurrentTeam()`.  
4. Optional: open the dashboard on the same profile.

## The moral

People should not have to ship a release to make a decision. Live **boolean-ish kill flag** belongs on the hub — and the Angular SDK is a peer on that hub.

— Kiponos · https://kiponos.io
