---
title: "React + Kiponos: API Rate Limit Knobs Without Bounce"
published: false
tags: react, typescript, devops, websocket, kiponos
description: Use the Kiponos React SDK to live-control RPS cap (`api/rate-limit-rps`) without redeploy. createFromEnv, Java parity, and a runnable monorepo path.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-react-sdk-rate-limit.md
---

# React + Kiponos: API Rate Limit Knobs Without Bounce

**Use case.** Control **RPS cap** at hub path `api/rate-limit-rps` from a **@kiponos/react** process peer — while Java and the dashboard stay in sync.

I have met teams that bounced pods just to change a rate limit integer. The fix is not another SPA build. It is a hub key.

## Package

https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-react-sdk

Companion: `examples/java/react-sdk-rate-limit`

## Code

```ts
import { Kiponos } from '@kiponos/react/server';

const kip = Kiponos.createFromEnv();
await kip.connect();
await kip.ensurePath('api');
await kip.path('api').set('rate-limit-rps', '100');
// live UI (BFF-injected client):
// const v = useKiponosValue('api/rate-limit-rps', { defaultValue: '100' });
```

## How to try

1. Export `KIPONOS_ID`, `KIPONOS_ACCESS`, `KIPONOS` (Connect UI).  
2. Build the SDK package under `sdks/kiponos-react-sdk`.  
3. Set `api/rate-limit-rps` from Node; read it from Java `createForCurrentTeam()`.  
4. Optional: open the dashboard on the same profile.

## The moral

People should not have to ship a release to make a decision. Live **RPS cap** belongs on the hub — and the React SDK is a peer on that hub.

— Kiponos · https://kiponos.io
