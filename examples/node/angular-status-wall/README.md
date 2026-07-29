# Angular/Node status-wall peer (`@kiponos/angular`)

Runnable **npm** example: Node (BFF/SSR) process joins the Kiponos hub with
`createFromEnv`, writes `demo/status-wall/*`. Angular UI injects a server-created
client — **never** Connect tokens in the SPA.

## Install

```bash
npm install @kiponos/angular
# or:
npm install
```

## Env (Connect UI — same as Java)

```bash
export KIPONOS_ID=…
export KIPONOS_ACCESS=…
export KIPONOS="['MyApp']['1.0']['Dev']['base']"
```

## Run (Node peer)

```bash
cd examples/node/angular-status-wall
npm install
npm start
node status-wall.mjs focus "from npm angular example"
```

## Angular UI (DI + Signals)

```ts
// app.config.ts — inject a client created on the server / BFF
import { provideKiponos } from '@kiponos/angular';
providers: [provideKiponos({ client })];

// component
import { injectKiponos } from '@kiponos/angular';
const kip = injectKiponos();
const status = kip.value('demo/status-wall/status-alpha', { defaultValue: 'idle' });
```

## Java peer

See `examples/java/angular-sdk-hub-peer`.

## Docs

- https://www.npmjs.com/package/@kiponos/angular  
- https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-angular-sdk  
