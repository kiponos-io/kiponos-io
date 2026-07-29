---
title: "React + Kiponos: Live Theme Toggle Without a SPA Redeploy"
published: false
tags: react, typescript, devops, websocket, kiponos
description: Use the Kiponos React SDK to live-control theme string on the hub (`ui/theme`) without redeploy. createFromEnv, Java parity, and a runnable Install with `npm install @kiponos/react`. Runnable example included.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-react-sdk-live-theme.md
---

# React + Kiponos: Live Theme Toggle Without a SPA Redeploy

**Use case.** Control **theme string on the hub** at hub path `ui/theme` from a **@kiponos/react** process peer — while Java and the dashboard stay in sync.

I have watched a design lead wait for a frontend deploy to flip dark mode for a demo. The fix is not another SPA build. It is a hub key.

## Package

```bash
npm install @kiponos/react
```

https://www.npmjs.com/package/@kiponos/react  
Source: https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-react-sdk

Companion: `examples/java/react-sdk-live-theme`

## Code

```ts
import { Kiponos } from '@kiponos/react/server';

const kip = Kiponos.createFromEnv();
await kip.connect();
await kip.ensurePath('ui');
await kip.path('ui').set('theme', 'dark');
// live UI (BFF-injected client):
// const v = useKiponosValue('ui/theme', { defaultValue: 'dark' });
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

People should not have to ship a release to make a decision. Live **theme string on the hub** belongs on the hub — and the React SDK is a peer on that hub.

— Kiponos · https://kiponos.io
