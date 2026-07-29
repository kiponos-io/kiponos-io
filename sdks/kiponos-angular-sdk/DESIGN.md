# Kiponos Angular SDK — Design

## Goal

Ship an **Angular package** that any Angular developer can install and use as a
first-class **real-time hub participant**, with API parity to the Java SDK —
same quality bar as `@kiponos/react`:

| Capability | Java | Angular SDK |
|------------|------|-------------|
| Connect with tokens + profile | `KIPONOS_ID` / `KIPONOS_ACCESS` | `createFromEnv` / env only |
| Navigate folders | `path("a","b")` | `path("a","b")` / `path("a/b")` |
| Get / set | `get` / `set` | `get` / `set` (Promise) |
| Create folders | `folderOrCreate` | `folderOrCreate` / `ensurePath` |
| Live hooks | `afterValueUpdated`, … | same names + Angular signals / RxJS |
| Local tree | in-memory after bootstrap | same; drives signal recompute |

Together with **Web dashboard**, **Java SDK**, **React SDK**, and **Python SDK**,
a change from any participant is reflected on all others instantly.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│  Angular app                                            │
│  provideKiponos({ client | fromEnv })                   │
│  inject(KiponosService) / injectKiponos()               │
│  kip.value('ui/theme')  // Signal                       │
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

1. **`core/`** — pure TS client (same protocol as React/Java/Python).
2. **`angular/`** — `provideKiponos` + `KiponosService` (signals + Observables).
3. **No NgRx required** — components inject the service; leaf signals re-read on tree change.

## Wire protocol

Identical to React/Java/Python SDK:

1. WebSocket to `wss://…/api/io-kiponos-sdk` with **HTTP headers** (Node) or query (browser, if server supports)
2. STOMP `CONNECT` `accept-version:1.2`, heartbeats
3. Binary zlib bootstrap on `/user/queue/sdk-boot`
4. Team topics: `config-val-updated`, `config-prop-saved`, …

### Outbound

| Action | Destination |
|--------|-------------|
| Save key | `/app/sdk-save-config-prop` |
| Create folder | `/app/sdk-create-config-folder` |
| Delete folder | `/app/sdk-delete-config-folder` |
| Delete key | `/app/delete-config-key` |

## Public API (developer-facing)

```ts
// app.config.ts (Node / SSR)
import { provideKiponos } from '@kiponos/angular';

export const appConfig = {
  providers: [
    provideKiponos({ fromEnv: true }),
  ],
};

// component
import { Component } from '@angular/core';
import { injectKiponos } from '@kiponos/angular';

@Component({
  selector: 'app-theme',
  template: `<span>{{ theme() }}</span>
             <button (click)="toggle()">Toggle</button>`,
})
export class ThemeComponent {
  private readonly kip = injectKiponos();
  theme = this.kip.value('ui/theme', { defaultValue: 'dark' });

  async toggle() {
    const cur = this.theme() ?? 'dark';
    await this.kip.path('ui').set('theme', cur === 'dark' ? 'light' : 'dark');
  }
}
```

### Client surface (Java parity)

```ts
Kiponos.createFromEnv()
Kiponos.createForCurrentTeam()

client.get(key, default?, ...folders)
client.set(key, value, ...folders)
client.path(...folders).folderOrCreate(name)
client.ensurePath(...folders)
client.afterValueUpdated(fn)
// … afterKeyCreated, afterKeyDeleted, afterKeyRenamed, afterItemSaved, afterFolderCreated
client.onChange(fn)
client.dump(...folders)
client.disconnect()
```

### Angular surface

| API | Role |
|-----|------|
| `provideKiponos(config)` | DI registration |
| `KiponosService` | Injectable façade |
| `injectKiponos()` | Short inject helper |
| `service.value(path)` | Live `Signal` for a leaf |
| `service.valueInt(path)` | Live int Signal |
| `service.changes()` | RxJS Observable of all deltas |
| `service.status` / `ready` | WritableSignal / computed |

## Security

- Never log token values.
- **Node/server-first:** credentials only via `KIPONOS_ID` / `KIPONOS_ACCESS` env (Java parity).
- Browser SPAs **must not** embed Connect tokens — BFF + SSE (or inject a server-side client).
- Direct `new KiponosClient({ tokens })` **throws** without factory gate.

## Workplan

| # | Task | Status |
|---|------|--------|
| 1 | Port core from React SDK (proven PROD protocol) | done |
| 2 | Angular provide + service + signals | done |
| 3 | Package build (tsup), types, README, example | done |
| 4 | Unit tests (paths/tree) | done |
| 5 | Live E2E suite vs PROD | pending QA run |

## Out of scope (v1)

- Offline / LKG modes (Java OfflineMode)
- Full dashboard UI operations
- npm publish (operator gate)
- Browser query-auth until server `SdkHandshake` deploy

## Naming

- **Package:** `@kiponos/angular` (repo `kiponos-angular-sdk`)
- **Version:** `0.1.0`
