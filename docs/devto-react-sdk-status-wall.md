---
title: "React + Kiponos: Shared Status Wall Across Peers"
published: false
tags: react, typescript, devops, websocket, kiponos
description: Use the Kiponos React SDK to live-control ops status leaf (`demo/status-wall/alpha`) without redeploy. createFromEnv, Java parity, and a runnable Install with `npm install @kiponos/react`. Runnable example included.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-react-sdk-status-wall.md
---

# React + Kiponos: Shared Status Wall Across Peers

**Use case.** Control **ops status leaf** at hub path `demo/status-wall/alpha` from a **@kiponos/react** process peer — while Java and the dashboard stay in sync.

I have always preferred a wall the whole team can see over a chat that scrolls away. The fix is not another SPA build. It is a hub key.

## Package

```bash
npm install @kiponos/react
```

https://www.npmjs.com/package/@kiponos/react  
Source: https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-react-sdk

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

```bash
# 1) Install the SDK from npm
npm install @kiponos/react

# 2) Connect credentials (same as Java)
export KIPONOS_ID=… KIPONOS_ACCESS=…
export KIPONOS="['MyApp']['1.0']['Dev']['base']"

# 3) Runnable Node peer
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/node/react-status-wall
npm install
npm start
# → writes demo/status-wall/* on the hub

# 4) Optional Java peer (same tree)
cd ../../java/react-sdk-hub-peer
# export same KIPONOS_* then run the Java example / tests
```

Package: [@kiponos/react on npm](https://www.npmjs.com/package/@kiponos/react)

## The moral

People should not have to ship a release to make a decision. Live **ops status leaf** belongs on the hub — and the React SDK is a peer on that hub.

— Kiponos · https://kiponos.io
