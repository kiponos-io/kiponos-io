---
title: "React + Kiponos: Incident Pause Flag the Agents Obey"
published: false
tags: react, typescript, devops, websocket, kiponos
description: Use the Kiponos React SDK to live-control pause flag for agents (`ops/incident/paused`) without redeploy. createFromEnv, Java parity, and a runnable monorepo path.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-react-sdk-incident-pause.md
---

# React + Kiponos: Incident Pause Flag the Agents Obey

**Use case.** Control **pause flag for agents** at hub path `ops/incident/paused` from a **@kiponos/react** process peer — while Java and the dashboard stay in sync.

I have drawn a line under automated remediation the moment a human said stop. The fix is not another SPA build. It is a hub key.

## Package

https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-react-sdk

Companion: `examples/java/react-sdk-incident-pause`

## Code

```ts
import { Kiponos } from '@kiponos/react/server';

const kip = Kiponos.createFromEnv();
await kip.connect();
await kip.ensurePath('ops', 'incident');
await kip.path('ops', 'incident').set('paused', 'no');
// live UI (BFF-injected client):
// const v = useKiponosValue('ops/incident/paused', { defaultValue: 'no' });
```

## How to try

1. Export `KIPONOS_ID`, `KIPONOS_ACCESS`, `KIPONOS` (Connect UI).  
2. Build the SDK package under `sdks/kiponos-react-sdk`.  
3. Set `ops/incident/paused` from Node; read it from Java `createForCurrentTeam()`.  
4. Optional: open the dashboard on the same profile.

## The moral

People should not have to ship a release to make a decision. Live **pause flag for agents** belongs on the hub — and the React SDK is a peer on that hub.

— Kiponos · https://kiponos.io
