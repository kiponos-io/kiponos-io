---
title: "React + Kiponos: Shared Status Wall Across Peers"
published: false
tags: react, typescript, devops, websocket, kiponos
description: Use the Kiponos React SDK to live-control ops status leaf (`demo/status-wall/alpha`) without redeploy. createFromEnv, Java parity, and a runnable monorepo path.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-react-sdk-status-wall.md
---

# React + Kiponos: Shared Status Wall Across Peers

**Use case.** Control **ops status leaf** at hub path `demo/status-wall/alpha` from a **@kiponos/react** process peer — while Java and the dashboard stay in sync.

I have always preferred a wall the whole team can see over a chat that scrolls away. The fix is not another SPA build. It is a hub key.

## Package

https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-react-sdk

Companion: `examples/java/react-sdk-status-wall`

## Code

```ts
import { Kiponos } from '@kiponos/react/server';

const kip = Kiponos.createFromEnv();
await kip.connect();
await kip.ensurePath('demo', 'status-wall');
await kip.path('demo', 'status-wall').set('alpha', 'idle');
// live UI (BFF-injected client):
// const v = useKiponosValue('demo/status-wall/alpha', { defaultValue: 'idle' });
```

## How to try

1. Export `KIPONOS_ID`, `KIPONOS_ACCESS`, `KIPONOS` (Connect UI).  
2. Build the SDK package under `sdks/kiponos-react-sdk`.  
3. Set `demo/status-wall/alpha` from Node; read it from Java `createForCurrentTeam()`.  
4. Optional: open the dashboard on the same profile.

## The moral

People should not have to ship a release to make a decision. Live **ops status leaf** belongs on the hub — and the React SDK is a peer on that hub.

— Kiponos · https://kiponos.io
