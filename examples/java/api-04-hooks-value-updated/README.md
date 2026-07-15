# Example api-04 — Hooks: react when a value is updated

| | |
|--|--|
| **Level** | Core |
| **App shape** | Standalone long-running `main` |
| **Industry** | Any service with live rate limits / ops knobs |
| **Pain** | “We poll a file / SIGHUP the process so config changes land” |
| **SDK** | `createForCurrentTeam`, `folderOrCreate`, `set` / `get`, **`afterValueUpdated`**, `disconnect` |

## Business problem

Ops changes `max-rps` on a dashboard or in a remote store. The process is already up.

Old rituals:

- Poll a YAML file every N seconds  
- Catch `SIGHUP` and re-read disk  
- Bounce the process “because that’s how we always did it”

That is **configuration hell as a sleep loop** — judgment arrives in the hub, but the running system only notices when a timer or a signal happens to fire.

## What this example does

A tiny long-running Java process connects to Kiponos and ensures:

```text
examples / hooks-value-updated / max-rps
```

It registers:

```java
kiponos.afterValueUpdated(event -> { /* apply new max-rps */ });
```

Then it “serves” for ~45 seconds, printing the live limit each tick.  
**Flip `max-rps` in the Kiponos dashboard** while it listens — the hook applies the new value **without restart, poll, or SIGHUP**.

## Why Kiponos fits

| Old world | Kiponos |
|-----------|---------|
| Poll files / watchers | Push event → `afterValueUpdated` |
| SIGHUP + re-read disk | Hub edit → in-process consumer |
| Restart to “pick up config” | Process stays up; posture updates live |
| Silent lag until next poll | Reaction when the value actually changes |

Real-time hub + hooks = the on-call brain and the worker share a nervous system.

## Prerequisites

1. Free [TeamPro](https://kiponos.io) account  
2. From **Connect**: `KIPONOS_ID`, `KIPONOS_ACCESS`, config profile path  

## Run

```bash
cd examples/java/api-04-hooks-value-updated

export KIPONOS_ID='…from Connect…'
export KIPONOS_ACCESS='…from Connect…'
# optional; default in build.gradle is my-app/v1.0.0/dev/base
export KIPONOS="['my-app']['v1.0.0']['dev']['base']"

./gradlew run
```

### Expected output (first run)

Creates `examples/hooks-value-updated/max-rps=50` if missing, registers the hook, prints worker ticks under the current limit. While it listens, change `max-rps` in the dashboard and watch:

```text
[hook] afterValueUpdated
  key:      max-rps
  applied:  max-rps 50 → 200
```

### Golden E2E test

```bash
./gradlew test
```

Live handshake + ensure `max-rps` + register `afterValueUpdated`. Skips if tokens are still `REPLACE_WITH_*`.

## Dashboard exercise

1. Open Kiponos.io → your env  
2. Find `examples → hooks-value-updated → max-rps`  
3. Start `./gradlew run`  
4. Set `max-rps` to another positive integer  
5. Watch the hook log and the next worker tick  

## Files

| Path | Role |
|------|------|
| `HooksValueUpdatedApp.java` | Demo main + hook handler |
| `HooksValueUpdatedLogicTest.java` | Pure parse / filter unit tests |
| `HooksValueUpdatedGoldenTest.java` | Live SDK golden |
| `build.gradle` | Dependency + env wiring |

## Next

See the full catalog: [`examples/CATALOG.md`](../../CATALOG.md)
