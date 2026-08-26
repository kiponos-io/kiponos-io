# agentic-cursor-session-shared-state

Shared Cursor / coding-agent session posture on the Kiponos hub

Live hub leaf for **session-posture** — a second agent (Claude Code / Grok Build) picks up the same live keys without paste, restart, or MCP reboot.

## Hub

```text
examples/agentic-cursor-session-shared-state/session-posture = focus=admin-wall,shopping-pause=off
```

Pain: Admin dashboard live wall + Shopping App incident pause stay trapped in one IDE session

## Run

```bash
cd examples/java/agentic-cursor-session-shared-state
cp kiponos.local.env.example kiponos.local.env   # from kiponos.io → Connect
./gradlew test run
```

## Peers

| Peer | Role |
|------|------|
| Java | this example — `Kiponos.createForCurrentTeam()` |
| Python | `Kiponos.connect()` agent peer |
| React | `@kiponos/react` **server** peer (`createFromEnv`) — never browser tokens |
| Angular | `@kiponos/angular` **server** peer — same moral |

dev.to essay: `docs/devto-agentic-cursor-session-shared-state.md`
