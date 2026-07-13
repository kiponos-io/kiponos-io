# Example 02 — Standalone Java: multi-env profile

| | |
|--|--|
| **Level** | Intro |
| **App shape** | Standalone `main` (no framework) |
| **Industry** | Any service shipped as one jar to many environments |
| **Pain** | “Same jar, wrong env file copied to prod” |
| **SDK** | `createForCurrentTeam`, profile via `-Dkiponos`, `folderOrCreate`, `get` / `set`, `disconnect` |

## Business problem

Release night. The artifact is correct. The **file** is not.

Someone promoted `application-staging.yml` with the jar. Or the Ansible role expanded the wrong template. Staging hostnames are now answering production traffic — or worse, production secrets never made it onto the box and the app is politely calling a sandbox.

That is configuration hell as **environment identity failure**: the binary is fine; the process does not know who it is.

## What this example does

A tiny Java process connects to Kiponos using the profile path:

```text
-Dkiponos=['my-app']['v1.0.0']['dev']['base']
```

It reads:

```text
examples / multi-env / env-label
examples / multi-env / api-base-url
```

Same jar. Change only the profile (env var `KIPONOS` or system property) for staging/prod. Env-specific values live in the hub under each profile — not in a file that can be copied wrong.

## Why Kiponos fits

| Old world | Kiponos |
|-----------|---------|
| `application-dev.yml` / `application-prod.yml` on disk | One profile path per process |
| Wrong file wins a merge | Hub values scoped to the profile you connect with |
| “Which conf is on this host?” | Print `-Dkiponos` + hub keys |
| Rebuild jar to retarget env | Same binary, different profile |

## Prerequisites

1. Free [TeamPro](https://kiponos.io) account  
2. From **Connect**: `KIPONOS_ID`, `KIPONOS_ACCESS`, and a profile path per environment  

## Run

```bash
cd examples/java/02-standalone-multi-env-profile

export KIPONOS_ID='…from Connect…'
export KIPONOS_ACCESS='…from Connect…'

# Dev (default in build.gradle if unset)
export KIPONOS="['my-app']['v1.0.0']['dev']['base']"
./gradlew run

# Staging — same jar, different profile
export KIPONOS="['my-app']['v1.0.0']['staging']['base']"
./gradlew run

# Prod
export KIPONOS="['my-app']['v1.0.0']['prod']['base']"
./gradlew run
```

### Expected output (first run on a profile)

Creates `examples/multi-env/env-label` and `api-base-url` defaults for that profile if missing, then prints profile + values.

### Golden E2E test

```bash
./gradlew test
```

Live handshake + ensure multi-env keys. Skips if tokens are still `REPLACE_WITH_*`.

## Dashboard exercise

1. Connect as **dev** → set `api-base-url` to your real dev API  
2. Connect as **staging** (separate profile in the hub) → set a different URL  
3. Run the same project twice with different `KIPONOS` — values must not leak across profiles  

## Files

| Path | Role |
|------|------|
| `MultiEnvProfileApp.java` | Demo main |
| `MultiEnvLogicTest.java` | Pure profile/label logic |
| `MultiEnvGoldenTest.java` | Live SDK golden |
| `build.gradle` | Dependency + env / profile wiring |

## Next

See the full catalog: [`examples/CATALOG.md`](../../CATALOG.md)
