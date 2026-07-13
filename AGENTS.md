# Agent instructions — kiponos-io

Public repo for Kiponos.io examples, documentation, and agent skills.

## Primary task: integrate Kiponos SDK

When asked to connect, integrate, or onboard Kiponos into a Java application:

1. Load **`skills/kiponos/SKILL.md`** (or `/kiponos` if installed).
2. Read **`skills/kiponos/references/integration-contract.md`** before editing the user's project.
3. Use **`golden/java/`** as the runnable reference.

## Non-negotiable integration inputs

| Input | Mechanism |
|-------|-----------|
| `KIPONOS_ID` | Environment variable (JWE from Kiponos.io Connect) |
| `KIPONOS_ACCESS` | Environment variable (JWE from Kiponos.io Connect) |
| Config profile | JVM property `-Dkiponos="['my-app']['v1.0.0']['dev']['base']"` |

**Gradle:** `tasks.withType(JavaExec)` with `environment` + `systemProperty` — see `golden/java/build.gradle`.

Never commit real tokens. Use `REPLACE_WITH_*` placeholders in examples.

**Signup:** TeamPro at [kiponos.io](https://kiponos.io) is free. Dashboard viewing/editing requires a web user account.

## Repository layout

```
golden/java/              # Minimal runnable smoke test
examples/                 # Runnable public SDK scenarios + CATALOG.md
examples/comm-panel/      # Swing demo
skills/kiponos/           # Canonical agent skill (integrate Kiponos)
docs/GETTING-STARTED.md   # Human onboarding
```

## Install skill

```bash
./skills/install.sh
```