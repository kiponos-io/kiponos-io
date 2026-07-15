# Example sre-degradation-mode — SRE “read-only mode” button

| | |
|--|--|
| **Level** | Intro |
| **App shape** | Standalone `main` (no framework) |
| **Industry** | Platform / SRE / any on-call production service |
| **Pain** | “We need a read-only mode button — without a redeploy” |
| **SDK** | `createForCurrentTeam`, `getRootFolder`, `folderOrCreate`, `set` / `get`, `disconnect` |

## Business problem

A dependency is limping. Disk is filling. A partner API is returning 503s like weather.  
On-call already **knows** the right move: stop accepting mutations, maybe park background jobs, keep the process alive for reads and diagnosis.

The old world still asks for a release train:

- Find three flags in three YAML files  
- Open a PR titled “temporary degradation”  
- Wait for CI  
- Deploy the “safe” artifact  
- Hope every replica got the same story  

That is **configuration hell** for SRE: a human judgment blocked by packaging.

## What this example does

A tiny Java process connects to Kiponos and reads a **kill-switch tree**:

```text
examples / sre-degradation-mode /
  mode              = full | read-only | maintenance
  accept-writes     = yes | no
  background-jobs   = yes | no
```

| Mode | Writes | Background jobs |
|------|--------|-----------------|
| `full` | honor `accept-writes` | honor `background-jobs` |
| `read-only` | **forced off** | honor `background-jobs` |
| `maintenance` | **forced off** | **forced off** |

Ops sets `mode` (and optional knobs) in the **Kiponos.io dashboard**. Re-run the app — **no jar rebuild**.

## Why Kiponos fits

| Old world | Kiponos |
|-----------|---------|
| Redeploy to enter read-only | Flip `mode` in the hub |
| Flags scattered across yml | One nested tree under `sre-degradation-mode` |
| “Which pod got the flag?” | Profile path + shared hub memory |
| Temporary PR that becomes permanent debt | Dashboard change with an explicit owner |

Real-time hub = the on-call brain is connected to the running process.

## Prerequisites

1. Free [TeamPro](https://kiponos.io) account  
2. From **Connect**: `KIPONOS_ID`, `KIPONOS_ACCESS`, config profile path  

## Run

```bash
cd examples/java/sre-degradation-mode

export KIPONOS_ID='…from Connect…'
export KIPONOS_ACCESS='…from Connect…'
# optional; default in build.gradle is my-app/v1.0.0/dev/base
export KIPONOS="['my-app']['v1.0.0']['dev']['base']"

./gradlew run
```

Or put placeholders into `build.gradle` the same way as `golden/java`.

### Expected output (first run)

Creates defaults (`mode=full`, `accept-writes=yes`, `background-jobs=yes`) if missing, then prints the effective posture.

### Golden E2E test

```bash
./gradlew test
```

Live handshake + ensure degradation tree keys. Skips if tokens are still `REPLACE_WITH_*`.

## Dashboard exercise

1. Open Kiponos.io → your env  
2. Find `examples → sre-degradation-mode`  
3. Set `mode` to `read-only`  
4. `./gradlew run` again → **READ-ONLY** posture (writes refused)  
5. Set `mode` to `maintenance` → writes **and** jobs off  

## Files

| Path | Role |
|------|------|
| `DegradationModeApp.java` | Demo main + posture resolution |
| `DegradationModeLogicTest.java` | Pure mode/knob unit tests |
| `DegradationModeGoldenTest.java` | Live SDK golden |
| `build.gradle` | Dependency + env wiring |

## Next

See the full catalog: [`examples/CATALOG.md`](../../CATALOG.md)
