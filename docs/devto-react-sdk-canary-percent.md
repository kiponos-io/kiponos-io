---
title: "React + Kiponos: Canary Percent Live on the Hub"
published: false
tags: react, typescript, devops, websocket, kiponos
description: Use the Kiponos React SDK to live-control traffic split percent (`release/canary-percent`) without redeploy. createFromEnv, Java parity, and a runnable monorepo path.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-react-sdk-canary-percent.md
---

# React + Kiponos: Canary Percent Live on the Hub

**Use case.** Control **traffic split percent** at hub path `release/canary-percent` from a **@kiponos/react** process peer — while Java and the dashboard stay in sync.

I have heard an on-call engineer say they needed five percent of traffic, not a full release train. The fix is not another SPA build. It is a hub key.

## Package

https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-react-sdk

Companion: `examples/java/react-sdk-canary-percent`

## Code

```ts
import { Kiponos } from '@kiponos/react/server';

const kip = Kiponos.createFromEnv();
await kip.connect();
await kip.ensurePath('release');
await kip.path('release').set('canary-percent', '5');
// live UI (BFF-injected client):
// const v = useKiponosValue('release/canary-percent', { defaultValue: '5' });
```

## How to try

1. Export `KIPONOS_ID`, `KIPONOS_ACCESS`, `KIPONOS` (Connect UI).  
2. Build the SDK package under `sdks/kiponos-react-sdk`.  
3. Set `release/canary-percent` from Node; read it from Java `createForCurrentTeam()`.  
4. Optional: open the dashboard on the same profile.

## The moral

People should not have to ship a release to make a decision. Live **traffic split percent** belongs on the hub — and the React SDK is a peer on that hub.

— Kiponos · https://kiponos.io
