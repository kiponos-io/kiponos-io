# Node examples (npm `@kiponos/*`)

| Example | Package | What it does |
|---------|---------|----------------|
| [react-status-wall](./react-status-wall/) | `@kiponos/react` | Node peer writes `demo/status-wall/*` |
| [angular-status-wall](./angular-status-wall/) | `@kiponos/angular` | Same wall via Angular server entry |

## Install (production)

```bash
npm install @kiponos/react
npm install @kiponos/angular
```

Registry: https://www.npmjs.com/org/kiponos (packages under user `kiponos`)

## Pattern

```text
Browser SPA  ↔  your Node BFF (createFromEnv)  ↔  Kiponos hub
                     ↑
              KIPONOS_ID / KIPONOS_ACCESS in process env only
```

Java peers: `examples/java/react-sdk-hub-peer`, `examples/java/angular-sdk-hub-peer`.

## Agentic wave

`examples/node/agentic-*-react` and `agentic-*-angular` — Node BFFs (`@kiponos/react/server`, `@kiponos/angular/server`). `npm test` is offline; `node peer.mjs --serve` exposes `GET /posture`. See [`../AGENTIC-WAVE.md`](../AGENTIC-WAVE.md).
