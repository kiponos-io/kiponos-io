# Agent instructions — kiponos-io

This repository is the public home for Kiponos.io examples, documentation, and agent skills.

## Primary task: integrate Kiponos SDK

When asked to connect, integrate, or onboard Kiponos into a Java application:

1. Load the skill at **`skills/kiponos/SKILL.md`** (or invoke `/kiponos` if installed).
2. Read **`skills/kiponos/references/integration-contract.md`** before editing the user's project.
3. Use **`golden/java/`** as the runnable reference implementation when present.

## Non-negotiable integration inputs

| Input | Mechanism |
|-------|-----------|
| `KIPONOS_ID` | Environment variable (JWE from Kiponos.io) |
| `KIPONOS_ACCESS` | Environment variable (JWE from Kiponos.io) |
| Config profile | JVM property `-Dkiponos="['app']['release']['env']['profile']"` |

Never commit tokens. Use `kiponos.local.gradle` (gitignored) for local dev.

## Repository layout (target)

```
golden/java/           # Minimal runnable Gradle example
examples/comm-panel/   # CommPanel demo app
skills/kiponos/        # Agent skill (Grok, Cursor, Claude, Copilot, …)
docs/                  # Human-facing guides
```

## Install skill locally

```bash
./skills/install.sh
```