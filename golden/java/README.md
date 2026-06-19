# Kiponos Golden — Java

Minimal runnable example: connect to Kiponos.io, read one config value, print it, disconnect.

**Prerequisite:** free [TeamPro account](https://kiponos.io) and tokens from the Connect screen.  
**Coming:** public read-only sandbox — [docs/PUBLIC-SANDBOX.md](../../docs/PUBLIC-SANDBOX.md).

## Quick start

1. Open `build.gradle` and replace:
   - `REPLACE_WITH_KIPONOS_ID_FROM_ACCOUNT`
   - `REPLACE_WITH_KIPONOS_ACCESS_FROM_ACCOUNT`
   - Profile if needed (default: `['my-app']['v1.0.0']['dev']['base']`)

2. Run:

```bash
./gradlew run
```

Expected: `springBootStarterURL: https://start.spring.io`

3. Change `starter` in your Kiponos dashboard → run again → new value. No restart.

## Download

[GitHub Releases](https://github.com/kiponos-io/kiponos-io/releases) — `golden-java.zip`

## Full guide

[`docs/GETTING-STARTED.md`](../../docs/GETTING-STARTED.md)

## Agent integration

[`skills/kiponos/SKILL.md`](../../skills/kiponos/SKILL.md) · [`AGENTS.md`](../../AGENTS.md)