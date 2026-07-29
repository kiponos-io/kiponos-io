# @kiponos/react

**Production install**

```bash
npm install https://github.com/kiponos-io/kiponos-io/releases/download/sdk-js-v0.1.0/kiponos-react-0.1.0.tgz
# or (npm 7+):
# npm install "github:kiponos-io/kiponos-io#master:sdks/kiponos-react-sdk"
```

**Node/server-first** Kiponos real-time SDK for React/Node backends
(Java `createForCurrentTeam` parity).

Connect identity comes **only from process environment** — never constructor
tokens, never browser SPA secrets.

## Install

```bash
npm install @kiponos/react
# peer: react >= 17 (for hooks/provider); Node >= 18; optional peer: ws
```

## Quick start (Node)

```bash
export KIPONOS_ID=…          # Connect UI
export KIPONOS_ACCESS=…
export KIPONOS="['MyApp']['1.0']['Dev']['base']"
# optional: KIPONOS_ENV_FILE=/path/to/dotenv
```

```ts
import { Kiponos } from '@kiponos/react';

// New instance from env
const kip = Kiponos.createFromEnv();
// or Java-style process singleton:
// const kip = Kiponos.createForCurrentTeam();

await kip.connect();
await kip.ensurePath('ui');
await kip.path('ui').set('theme', 'light');
console.log(kip.get('theme', undefined, 'ui'));

kip.afterValueUpdated((e) => console.log(e.key, e.value));
```

## React on a server / BFF pattern

Browser SPAs **must not** embed Connect tokens. Pattern:

```text
Browser React  ↔  your Node API (createFromEnv + SSE)  ↔  Kiponos hub
```

```tsx
// Node only — create client once
import { Kiponos, KiponosProvider } from '@kiponos/react';
const client = Kiponos.createForCurrentTeam();
await client.connect();

// Pass client into server-rendered tree, or expose SSE to the SPA
<KiponosProvider client={client}>…</KiponosProvider>
```

`KiponosProvider` without `client` + `fromEnv` only works in **Node** (reads env). In the browser, inject `client` from your backend session or use SSE (see Mirror Phone service).

## Env vars

| Variable | Required | Purpose |
|----------|----------|---------|
| `KIPONOS_ID` | yes | Connect identity token |
| `KIPONOS_ACCESS` | yes | Connect access token |
| `KIPONOS` | recommended | Profile `['App']['rel']['env']['cfg']` |
| `KIPONOS_ENV_FILE` | no | Dotenv path (e.g. otp-listener.env) |
| `KIPONOS_SERVER` | no | Default `wss://kiponos.io/api/io-kiponos-sdk` |

## Not supported

- Passing tokens into `new KiponosClient({ idToken, accessToken })` — **throws**
- Browser `createFromEnv` — **throws** (use BFF)

## Tests

```bash
npm test          # unit
npm run test:e2e  # live PROD via createFromEnv
```

## Design / QA

- [DESIGN.md](./DESIGN.md)
- [QA_REPORT.md](./QA_REPORT.md)

## License

MIT
