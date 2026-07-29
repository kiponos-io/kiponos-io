# Kiponos React SDK — Design

## Goal

Ship a **React package** that any React developer can install and use as a
first-class **real-time hub participant**, with API parity to the Java SDK:

| Capability | Java | React SDK |
|------------|------|-----------|
| Connect with tokens + profile | `KIPONOS_ID` / `KIPONOS_ACCESS` | `idToken` / `accessToken` + `profile` |
| Navigate folders | `path("a","b")` | `path("a","b")` / `path("a/b")` |
| Get / set | `get` / `set` | `get` / `set` (Promise) |
| Create folders | `folderOrCreate` | `folderOrCreate` / `ensurePath` |
| Live hooks | `afterValueUpdated`, `afterKeyCreated`, … | same names + React hooks |
| Local tree | in-memory after bootstrap | same; drives React re-renders |

Together with **Web dashboard**, **Java SDK**, and **Python SDK**, a change from
any participant is reflected on all others instantly.

## Why not extract kiponos-web wholesale?

`kiponos-web` is the **operator dashboard**:

| | Web app | SDK participant |
|--|---------|-----------------|
| Endpoint | `/api/io-kiponos-websocket` (SockJS) | `/api/io-kiponos-sdk` (native WS) |
| Auth | Session / cookies / user JWTs | `sdk-id-token`, `sdk-access-token`, `kiponos-id` |
| Destinations | `/app/save-config-prop`, … | `/app/sdk-save-config-prop`, … |
| Bootstrap | REST configs + topics | Binary zlib STOMP `/user/queue/sdk-boot` |
| Role | Human UI | App process peer |

We **reuse patterns** from web (subscribe queue, team topics, send helpers) but
implement the **SDK wire protocol** (aligned with Python `agent_client.py` and
Java ReadyMode).

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│  React app                                              │
│  <KiponosProvider profile tokens>                       │
│    useKiponos() / useKiponosValue() / listeners         │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│  KiponosClient (framework-agnostic)                     │
│  · local config tree                                    │
│  · path / get / set / folderOrCreate                    │
│  · after* listener registry                             │
│  · requestId wait for save/create acks                  │
└────────────────────────┬────────────────────────────────┘
                         │ STOMP 1.2 + heartbeats
┌────────────────────────▼────────────────────────────────┐
│  wss://kiponos.io/api/io-kiponos-sdk                    │
│  HTTP handshake: sdk-id-token, sdk-access-token,        │
│                  kiponos-id, sdk-version                │
└─────────────────────────────────────────────────────────┘
```

### Layers

1. **`core/`** — pure TS client (usable from Node, workers, non-React).
2. **`react/`** — `KiponosProvider` + hooks that subscribe to client events.
3. **No Context tree for app state** — components call hooks against the hub;
   only the Provider holds the connection.

## Wire protocol (from Python/Java)

### Connect

1. WebSocket to `wss://…/api/io-kiponos-sdk`  
   - **Java/Python**: HTTP headers `sdk-id-token`, `sdk-access-token`, `kiponos-id`, `sdk-version`  
   - **Browser**: same values as **query params** (browsers cannot set WS headers)  
     → requires server `SdkHandshake` query-param fallback (see `server-notes/`)
2. STOMP `CONNECT` `accept-version:1.2`, `heart-beat:10000,10000`
3. `SUBSCRIBE` `/user/queue/sdk-boot`
4. Receive **binary** MESSAGE → raw inflate (zlib) → JSON `[configTree, teamInfo]`
5. Subscribe team topics:
   - `config-val-updated`, `config-prop-saved`, `config-key-created`
   - `config-key-deleted`, `config-key-renamed`
   - `config-folder-created`, `config-folder-deleted`

### Outbound

| Action | Destination | Body (essentials) |
|--------|-------------|-------------------|
| Save key | `/app/sdk-save-config-prop` | `requestId`, `basePath`, `key`, `value`, `oldKey` |
| Create folder | `/app/sdk-create-config-folder` | `requestId`, `path`, `folder` |
| Delete folder | `/app/sdk-delete-config-folder` | `requestId`, `basePath`, `folderName` |
| Delete key | `/app/delete-config-key` | `requestId`, `basePath`, `key` |

`basePath` = JsonPath under profile root, e.g.  
`$.rootAccount['apps']['MyApp']['rels']['1.0']['envs']['Dev']['cfgs']['base']['ui']`

### Tree shape

Keys are leaf objects `{ "value": "<string>" }`. Folders are plain objects
without a sole `value` key. Matches Java/Python.

## Public API (developer-facing)

```tsx
import {
  KiponosProvider,
  useKiponos,
  useKiponosValue,
  KiponosClient,
} from '@kiponos/react';

// Root
<KiponosProvider
  profile="['MyApp']['1.0.0']['Dev']['base']"
  idToken={import.meta.env.VITE_KIPONOS_ID}
  accessToken={import.meta.env.VITE_KIPONOS_ACCESS}
>
  <App />
</KiponosProvider>

// Anywhere (no parent Context needed for domain state)
function ThemeBadge() {
  const theme = useKiponosValue('ui/theme', { defaultValue: 'dark' });
  return <span>{theme}</span>;
}

function Admin() {
  const kip = useKiponos();
  return (
    <button onClick={() => kip.path('ui').set('theme', 'light')}>
      Light
    </button>
  );
}
```

### Client surface

```ts
client.get(key, default?, ...folders)
client.getPath('a/b/key', default?)
client.set(key, value, ...folders)          // Promise<string>
client.setPath('a/b/key', value)
client.path(...folders)                     // KiponosFolder
client.folderOrCreate(name)                 // async / chainable promise API
client.ensurePath(...folders)
client.afterValueUpdated(fn)
client.afterKeyCreated(fn)
client.afterKeyDeleted(fn)
client.afterKeyRenamed(fn)
client.afterItemSaved(fn)
client.afterFolderCreated(fn)
client.onChange(fn)                         // catch-all like Python
client.dump(...folders)
client.disconnect()
```

## React re-render model

- Provider owns one `KiponosClient` instance.
- Client emits internal `tree-changed` / typed events.
- `useKiponosValue(path)` selects a leaf and re-renders only when that leaf changes.
- `useKiponos()` returns stable methods + `status` / `ready` / `error`.

This replaces deep React Context trees for **shared live state**.

## Security

- Never log token values.
- Prefer env / secure vault injection into Provider props.
- Browser query-param handshake: tokens may appear in access logs; plan a
  short-lived ticket exchange later. Document for operators.
- Tokens are **Connect SDK tokens** (same as Java/Python), not dashboard user cookies.

## Workplan

| # | Task | Status |
|---|------|--------|
| 1 | Study web WS + Java/Python protocol | done |
| 2 | Design doc | done |
| 3 | Core: paths, tree, STOMP client, KiponosClient | done |
| 4 | React Provider + hooks | done |
| 5 | Package build (tsup), types, README, example | done |
| 6 | Unit tests (paths/tree) | done (7) |
| 7 | Server note: query-param handshake for browsers | done |
| 8 | Live E2E suite vs PROD (no mocks) | done — 37/37 PASS; see QA_REPORT.md |

## Out of scope (v1)

- Offline / LKG modes (Java has OfflineMode)
- Full dashboard UI operations (invite, chat, insight)
- Server deploy of handshake patch (local note + optional patch only)
- npm publish (repo ready; publish when you approve)

## Naming

- **Package:** `@kiponos/react` (npm scope; local `kiponos-react-sdk` repo)
- **Participant type:** browser/React SDK client (same hub as Java/Python)
- **Version:** `0.1.0`
