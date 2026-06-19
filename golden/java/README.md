# Kiponos Golden — Java

Minimal runnable example: connect to Kiponos.io, read one config value, print it, disconnect.

## Quick start

1. Open `build.gradle` and replace the three placeholders in the `JavaExec` block:
   - `REPLACE_WITH_KIPONOS_ID_FROM_ACCOUNT`
   - `REPLACE_WITH_KIPONOS_ACCESS_FROM_ACCOUNT`
   - `['my-app']['v1.0.0']['dev']['base']` (defaults — change to match your account)

   Copy values from your Kiponos.io account → **Connect** / SDK setup screen.

2. Run:

```bash
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
- `tasks.withType(JavaExec)` — self-contained credentials in `build.gradle`, isolated from global shell env

## Agent integration

See [`skills/kiponos/SKILL.md`](../../skills/kiponos/SKILL.md) and root [`AGENTS.md`](../../AGENTS.md).