---
title: "React + Kiponos: Feature Kill Switch From the Hub"
published: false
tags: react, typescript, devops, websocket, kiponos
description: Use the Kiponos React SDK to live-control boolean-ish kill flag (`flags/feature-x`) without redeploy. createFromEnv, Java parity, and a runnable monorepo path.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-react-sdk-feature-kill.md
---

# React + Kiponos: Feature Kill Switch From the Hub

**Use case.** Control **boolean-ish kill flag** at hub path `flags/feature-x` from a **@kiponos/react** process peer — while Java and the dashboard stay in sync.

I have sat through a war room where the only fix was a redeploy to turn a feature off. The fix is not another SPA build. It is a hub key.

## Package

https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-react-sdk

Companion: `examples/java/react-sdk-feature-kill`

## Code

```ts
import { Kiponos } from '@kiponos/react/server';

const kip = Kiponos.createFromEnv();
await kip.connect();
await kip.ensurePath('flags');
await kip.path('flags').set('feature-x', 'off');
// live UI (BFF-injected client):
// const v = useKiponosValue('flags/feature-x', { defaultValue: 'off' });
```

## How to try

1. Export `KIPONOS_ID`, `KIPONOS_ACCESS`, `KIPONOS` (Connect UI).  
2. Build the SDK package under `sdks/kiponos-react-sdk`.  
3. Set `flags/feature-x` from Node; read it from Java `createForCurrentTeam()`.  
4. Optional: open the dashboard on the same profile.

## The moral

People should not have to ship a release to make a decision. Live **boolean-ish kill flag** belongs on the hub — and the React SDK is a peer on that hub.

— Kiponos · https://kiponos.io
