# react-sdk-incident-pause

Java peer for the **@kiponos/react** npm SDK as the write/UI side.

## Install the JS peer (npm)

```bash
npm install @kiponos/react
```

Runnable Node example: `examples/node/react-status-wall`  
Package: https://www.npmjs.com/package/@kiponos/react

## Hub key

`ops/incident/paused` (default `no`)

## Run (Java)

```bash
export KIPONOS_ID=… KIPONOS_ACCESS=…
export KIPONOS="['MyApp']['1.0']['Dev']['base']"
# ./gradlew test   # when wired like sibling examples
```

## Run (Node peer)

```bash
cd examples/node/react-status-wall
npm install
npm start
```

See draft: `docs/examples/medium-drafts/react-sdk-incident-pause.md`
