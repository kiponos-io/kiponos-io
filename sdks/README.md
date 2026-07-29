# Kiponos front-end SDKs (public production)

| Package | Path | Install |
|---------|------|---------|
| `@kiponos/angular` | [kiponos-angular-sdk](./kiponos-angular-sdk/) | see below |
| `@kiponos/react` | [kiponos-react-sdk](./kiponos-react-sdk/) | see below |

Both follow **Java SDK API parity**: process-env identity, `path` / `get` / `set`, `folderOrCreate`, `after*` listeners, STOMP hub protocol.

**Security:** Connect tokens never live in browser SPA bundles. Use a Node BFF / SSR process.

## Install (production)

### Option A — Release tarball (recommended)

```bash
# Angular
npm install https://github.com/kiponos-io/kiponos-io/releases/download/sdk-js-v0.1.0/kiponos-angular-0.1.0.tgz

# React
npm install https://github.com/kiponos-io/kiponos-io/releases/download/sdk-js-v0.1.0/kiponos-react-0.1.0.tgz
```

### Option B — Git monorepo path (npm 7+)

```bash
npm install "github:kiponos-io/kiponos-io#master:sdks/kiponos-angular-sdk"
npm install "github:kiponos-io/kiponos-io#master:sdks/kiponos-react-sdk"
```

### Option C — npm registry

When `@kiponos/*` is on npmjs.com:

```bash
npm install @kiponos/angular
npm install @kiponos/react
```

*(npmjs publish needs an org token; **release tarballs are production** today.)*

Homepage: https://kiponos.io
