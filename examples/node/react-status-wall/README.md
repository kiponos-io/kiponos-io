# React/Node status-wall peer (`@kiponos/react`)

Runnable **npm** example: a Node process joins the Kiponos hub the same way Java
does (`createFromEnv` / process env), and writes `demo/status-wall/*`.

## Install

```bash
npm install @kiponos/react
# or from this folder (pulls the same registry package):
npm install
```

## Env (Connect UI — same as Java)

```bash
export KIPONOS_ID=…          # never commit
export KIPONOS_ACCESS=…
export KIPONOS="['MyApp']['1.0']['Dev']['base']"
```

## Run

```bash
cd examples/node/react-status-wall
npm install
npm start
# or:
node status-wall.mjs focus "from npm example"
```

## Java peer (same hub keys)

See `examples/java/react-sdk-hub-peer` — Java `createForCurrentTeam()` reads the
same leaves without a redeploy.

## Docs

- Package: https://www.npmjs.com/package/@kiponos/react  
- Source: https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-react-sdk  

**Never** put Connect tokens in a browser SPA bundle. Browser UIs talk to your
Node BFF; the BFF holds the SDK.
