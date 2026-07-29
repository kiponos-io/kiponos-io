---
title: "Angular + Kiponos: Incident Pause Flag the Agents Obey"
published: false
tags: angular, typescript, devops, websocket, kiponos
description: Use the Kiponos Angular SDK to live-control pause flag for agents (`ops/incident/paused`) without redeploy. createFromEnv, Java parity, and a runnable monorepo path.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-angular-sdk-incident-pause.md
---

# Angular + Kiponos: Incident Pause Flag the Agents Obey

**Use case.** Control **pause flag for agents** at hub path `ops/incident/paused` from a **@kiponos/angular** process peer — while Java and the dashboard stay in sync.

I have drawn a line under automated remediation the moment a human said stop. The fix is not another SPA build. It is a hub key.

## Package

https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-angular-sdk

Companion: `examples/java/angular-sdk-incident-pause`

## Code

```ts
import { Kiponos, injectKiponos, provideKiponos } from '@kiponos/angular';

// Node BFF
const client = Kiponos.createFromEnv();
await client.connect();
await client.path('ops', 'incident').set('paused', 'no');

// Angular component (client injected via provideKiponos)
// readonly v = injectKiponos().value('ops/incident/paused', { defaultValue: 'no' });
```

## How to try

1. Export `KIPONOS_ID`, `KIPONOS_ACCESS`, `KIPONOS` (Connect UI).  
2. Build the SDK package under `sdks/kiponos-angular-sdk`.  
3. Set `ops/incident/paused` from Node; read it from Java `createForCurrentTeam()`.  
4. Optional: open the dashboard on the same profile.

## The moral

People should not have to ship a release to make a decision. Live **pause flag for agents** belongs on the hub — and the Angular SDK is a peer on that hub.

— Kiponos · https://kiponos.io
