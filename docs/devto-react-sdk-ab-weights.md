---
title: "React + Kiponos: A/B Checkout Weights in Real Time"
published: false
tags: react, typescript, devops, websocket, kiponos
description: Use the Kiponos React SDK to live-control experiment weight (`checkout/ab-weight-b`) without redeploy. createFromEnv, Java parity, and a runnable monorepo path.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-react-sdk-ab-weights.md
---

# React + Kiponos: A/B Checkout Weights in Real Time

**Use case.** Control **experiment weight** at hub path `checkout/ab-weight-b` from a **@kiponos/react** process peer — while Java and the dashboard stay in sync.

I have watched product wait overnight for an A/B weight change that should have been a hub key. The fix is not another SPA build. It is a hub key.

## Package

https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-react-sdk

Companion: `examples/java/react-sdk-ab-weights`

## Code

```ts
import { Kiponos } from '@kiponos/react/server';

const kip = Kiponos.createFromEnv();
await kip.connect();
await kip.ensurePath('checkout');
await kip.path('checkout').set('ab-weight-b', '50');
// live UI (BFF-injected client):
// const v = useKiponosValue('checkout/ab-weight-b', { defaultValue: '50' });
```

## How to try

1. Export `KIPONOS_ID`, `KIPONOS_ACCESS`, `KIPONOS` (Connect UI).  
2. Build the SDK package under `sdks/kiponos-react-sdk`.  
3. Set `checkout/ab-weight-b` from Node; read it from Java `createForCurrentTeam()`.  
4. Optional: open the dashboard on the same profile.

## The moral

People should not have to ship a release to make a decision. Live **experiment weight** belongs on the hub — and the React SDK is a peer on that hub.

— Kiponos · https://kiponos.io
