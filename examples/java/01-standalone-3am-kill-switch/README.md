# Example 01 — Standalone Java: the 3am kill switch

| | |
|--|--|
| **Level** | Intro |
| **App shape** | Standalone `main` (no framework) |
| **Industry** | FinTech / any on-call production service |
| **Pain** | “We can’t disable payments without a redeploy” |
| **SDK** | `createForCurrentTeam`, `getRootFolder`, `folderOrCreate`, `set` / `get`, `disconnect` |

## Business problem

It is 03:17. A card processor is timing out. Money is not moving; support is on fire.  
The “fix” is a boolean buried in `application-prod.yml` on a branch someone must PR, wait for CI, and roll out.

That is **configuration hell** in its purest form: a human decision blocked by a release train.

## What this example does

A tiny Java process connects to Kiponos and reads:

```text
examples / kill-switch / payments-enabled
```

- `yes` / `true` → process behaves as if payments are online  
- `no` / `false` → process refuses new charges (safe posture)

Ops flips the key in the **Kiponos.io dashboard**. Re-run the app (or keep a long-running loop in later examples) — **no jar rebuild**.

## Why Kiponos fits

| Old world | Kiponos |
|-----------|---------|
| Edit file → commit → deploy | Edit key in hub → live memory |
| Wrong file on wrong host | One profile per env (`-Dkiponos=...`) |
| “Who changed prod at 3am?” | Dashboard history + optional `dumpConfig` (later examples) |

Real-time hub = the on-call brain is connected to the running process.

## Prerequisites

1. Free [TeamPro](https://kiponos.io) account  
2. From **Connect**: `KIPONOS_ID`, `KIPONOS_ACCESS`, config profile path  

## Run

```bash
cd examples/java/01-standalone-3am-kill-switch

export KIPONOS_ID='…from Connect…'
export KIPONOS_ACCESS='…from Connect…'
# optional; default in build.gradle is my-app/v1.0.0/dev/base
export KIPONOS="['my-app']['v1.0.0']['dev']['base']"

./gradlew run
```

Or put placeholders into `build.gradle` the same way as `golden/java`.

### Expected output (first run)

Creates `examples/kill-switch/payments-enabled=yes` if missing, then prints whether payments are enabled.

### Golden E2E test

```bash
./gradlew test
```

Live handshake + ensure kill-switch key. Skips if tokens are still `REPLACE_WITH_*`.

## Dashboard exercise

1. Open Kiponos.io → your env  
2. Find `examples → kill-switch → payments-enabled`  
3. Set to `no`  
4. `./gradlew run` again → **DISABLED** posture  

## Files

| Path | Role |
|------|------|
| `KillSwitchApp.java` | Demo main |
| `KillSwitchGoldenTest.java` | Live SDK golden |
| `build.gradle` | Dependency + env wiring |

## Next

See the full catalog: [`examples/CATALOG.md`](../../CATALOG.md)
