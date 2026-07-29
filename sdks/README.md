# Kiponos front-end SDKs (public)

| Package | Path | Notes |
|---------|------|--------|
| `@kiponos/angular` | [kiponos-angular-sdk](./kiponos-angular-sdk/) | Angular DI + Signals; Node `createFromEnv` |
| `@kiponos/react` | [kiponos-react-sdk](./kiponos-react-sdk/) | React hooks + Node `createFromEnv` |

Both follow **Java SDK API parity**: process-env identity, `path` / `get` / `set`, `folderOrCreate`, `after*` listeners, STOMP hub protocol.

**Security:** Connect tokens never live in browser SPA bundles. Use a Node BFF / SSR process.

```bash
cd sdks/kiponos-angular-sdk && npm install && npm test && npm run build
cd sdks/kiponos-react-sdk && npm install && npm test && npm run build
```

Homepage: https://kiponos.io
