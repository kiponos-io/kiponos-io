# Kiponos front-end SDKs (public production)

| Package | npm | Source | Example |
|---------|-----|--------|---------|
| `@kiponos/angular` | [npmjs](https://www.npmjs.com/package/@kiponos/angular) | [kiponos-angular-sdk](./kiponos-angular-sdk/) | `examples/node/angular-status-wall` |
| `@kiponos/react` | [npmjs](https://www.npmjs.com/package/@kiponos/react) | [kiponos-react-sdk](./kiponos-react-sdk/) | `examples/node/react-status-wall` |

## Install

```bash
npm install @kiponos/angular
npm install @kiponos/react
```

Java API parity: `createFromEnv` / `createForCurrentTeam`, path/get/set, after* listeners.

**Security:** Connect tokens never in browser SPA bundles — use a Node BFF.

Homepage: https://kiponos.io
