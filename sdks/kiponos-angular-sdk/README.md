# @kiponos/angular

**Production install**

```bash
npm install @kiponos/angular
```

Also on GitHub release / monorepo source under `sdks/kiponos-angular-sdk`.

## Install

```bash
npm install @kiponos/angular
# peers: @angular/core >= 16, rxjs >= 7 (for DI/service); Node >= 18; optional: ws
```

## Quick start (Node)

```bash
export KIPONOS_ID=…          # Connect UI
export KIPONOS_ACCESS=…
export KIPONOS="['MyApp']['1.0']['Dev']['base']"
# optional: KIPONOS_ENV_FILE=/path/to/dotenv
```

```ts
import { Kiponos } from '@kiponos/angular';

const kip = Kiponos.createFromEnv();
// or Java-style process singleton:
// const kip = Kiponos.createForCurrentTeam();

await kip.connect();
await kip.ensurePath('ui');
await kip.path('ui').set('theme', 'light');
console.log(kip.get('theme', undefined, 'ui'));

kip.afterValueUpdated((e) => console.log(e.key, e.value));
```

## Angular (standalone)

```ts
// app.config.ts
import { ApplicationConfig } from '@angular/core';
import { provideKiponos } from '@kiponos/angular';
import { Kiponos } from '@kiponos/angular';

// Preferred: create client in Node, inject into Angular (SSR / BFF)
const client = Kiponos.createForCurrentTeam();
await client.connect();

export const appConfig: ApplicationConfig = {
  providers: [
    provideKiponos({ client, autoConnect: false }),
    // Node-only alternative (throws in browser):
    // provideKiponos({ fromEnv: true }),
  ],
};
```

```ts
// theme.component.ts
import { Component } from '@angular/core';
import { injectKiponos } from '@kiponos/angular';

@Component({
  standalone: true,
  selector: 'app-theme',
  template: `
    <span>theme={{ theme() }}</span>
    <button [disabled]="!kip.ready()" (click)="toggle()">Toggle</button>
  `,
})
export class ThemeComponent {
  readonly kip = injectKiponos();
  readonly theme = this.kip.value('ui/theme', { defaultValue: 'dark' });

  async toggle() {
    const cur = this.theme() ?? 'dark';
    await this.kip.path('ui').set('theme', cur === 'dark' ? 'light' : 'dark');
  }
}
```

### Browser SPA pattern

Browser SPAs **must not** embed Connect tokens:

```text
Browser Angular  ↔  your Node API (createFromEnv + SSE)  ↔  Kiponos hub
```

Pass a server-created `client` into `provideKiponos({ client })`, or mirror
hub state over SSE.

## Env vars

| Variable | Required | Purpose |
|----------|----------|---------|
| `KIPONOS_ID` | yes | Connect identity token |
| `KIPONOS_ACCESS` | yes | Connect access token |
| `KIPONOS` | recommended | Profile `['App']['rel']['env']['cfg']` |
| `KIPONOS_ENV_FILE` | no | Dotenv path |
| `KIPONOS_SERVER` | no | Default `wss://kiponos.io/api/io-kiponos-sdk` |

## Not supported

- Passing tokens into `new KiponosClient({ idToken, accessToken })` — **throws**
- Browser `createFromEnv` — **throws** (use BFF)

## Java API map

| Java | Angular / TS |
|------|----------------|
| `Kiponos.createForCurrentTeam()` | `Kiponos.createForCurrentTeam()` |
| `path("a","b").set("k","v")` | `path("a","b").set("k","v")` |
| `folderOrCreate("x")` | `path().folderOrCreate("x")` |
| `afterValueUpdated(...)` | `afterValueUpdated(...)` / `KiponosService` |
| `get` / `getInt` | `get` / `valueInt()` signal |

## Tests

```bash
npm test          # unit
npm run test:e2e  # live PROD via createFromEnv
```

## Design / QA

- [DESIGN.md](./DESIGN.md)
- [QA_REPORT.md](./QA_REPORT.md) (after live run)

## License

MIT
