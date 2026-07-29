# Kiponos front-end SDKs (public production)

| Package | npm | Source |
|---------|-----|--------|
| `@kiponos/angular` | [npmjs.com/package/@kiponos/angular](https://www.npmjs.com/package/@kiponos/angular) | [kiponos-angular-sdk](./kiponos-angular-sdk/) |
| `@kiponos/react` | [npmjs.com/package/@kiponos/react](https://www.npmjs.com/package/@kiponos/react) | [kiponos-react-sdk](./kiponos-react-sdk/) |

## Install (production)

```bash
npm install @kiponos/angular
npm install @kiponos/react
```

Java API parity: `createFromEnv` / `createForCurrentTeam`, path/get/set, after* listeners.

**Security:** Connect tokens never live in browser SPA bundles. Use a Node BFF / SSR process.

Homepage: https://kiponos.io
