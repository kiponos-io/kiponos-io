---
main_image: https://files.catbox.moe/uiggaj.jpg
title: "Angular + Kiponos: Incident Pause Flag the Agents Obey"
published: false
tags: angular, typescript, devops, websocket, kiponos
description: Use the Kiponos Angular SDK to live-control pause flag for agents (`ops/incident/paused`) without redeploy. createFromEnv, Java parity, and a runnable Install with `npm install @kiponos/angular`. Runnable example included.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-angular-sdk-incident-pause.md
---

# Angular + Kiponos: Incident Pause Flag the Agents Obey

**Use case.** Control **pause flag for agents** at hub path `ops/incident/paused` from a **@kiponos/angular** process peer — while Java and the dashboard stay in sync.

I have drawn a line under automated remediation the moment a human said stop. The fix is not another SPA build. It is a hub key.

## Package

```bash
npm install @kiponos/angular
```

https://www.npmjs.com/package/@kiponos/angular  
Source: https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-angular-sdk

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

```bash
# 1) Install the SDK from npm
npm install @kiponos/angular

# 2) Connect credentials (same as Java)
export KIPONOS_ID=… KIPONOS_ACCESS=…
export KIPONOS="['MyApp']['1.0']['Dev']['base']"

# 3) Runnable Node peer (BFF process)
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/node/angular-status-wall
npm install
npm start
# → writes demo/status-wall/* on the hub

# 4) Optional Java peer (same tree)
cd ../../java/angular-sdk-hub-peer
```

Package: [@kiponos/angular on npm](https://www.npmjs.com/package/@kiponos/angular)

## The moral

People should not have to ship a release to make a decision. Live **pause flag for agents** belongs on the hub — and the Angular SDK is a peer on that hub.

— Kiponos · https://kiponos.io
