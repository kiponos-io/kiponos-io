# angular-sdk-feature-kill

Java peer for the **@kiponos/angular** npm SDK as the write/UI side.

## Install the JS peer (npm)

```bash
npm install @kiponos/angular
```

Runnable Node example: `examples/node/angular-status-wall`  
Package: https://www.npmjs.com/package/@kiponos/angular

## Hub key

`flags/feature-x` (default `off`)

## Run (Java)

```bash
export KIPONOS_ID=… KIPONOS_ACCESS=…
export KIPONOS="['MyApp']['1.0']['Dev']['base']"
# ./gradlew test   # when wired like sibling examples
```

## Run (Node peer)

```bash
cd examples/node/angular-status-wall
npm install
npm start
```

See draft: `docs/examples/medium-drafts/angular-sdk-feature-kill.md`
