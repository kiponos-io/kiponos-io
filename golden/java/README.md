# Kiponos Golden — Java

Minimal runnable example: connect to Kiponos.io, read one config value, print it, disconnect.

## Quick start

```bash
cp kiponos.local.gradle.example kiponos.local.gradle
# Edit kiponos.local.gradle with tokens + profile from Kiponos.io Connect

./gradlew run
```

Expected output includes:

```
springBootStarterURL: https://start.spring.io
```

## What it demonstrates

- `Kiponos.createForCurrentTeam()` — team-scoped client from env tokens
- `kiponos.path(...).get("key")` — folder-based config lookup
- `kiponos.disconnect()` — clean shutdown
- `kiponos.local.gradle` — self-contained credentials without polluting shell env

## Agent integration

See [`skills/kiponos/SKILL.md`](../../skills/kiponos/SKILL.md) and root [`AGENTS.md`](../../AGENTS.md).