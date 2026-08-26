# agentic-frameworks-missing-hub

The live hub agent frameworks do not ship

Live hub leaf for **shared-truth** — change it without restarting Java services, Python agents, or MCP hosts (Grok Build, Cursor, Claude Code).

## Hub

```text
examples/agentic-frameworks-missing-hub/shared-truth = live
```

Pain: Tools and MCP without a live shared tree still force restarts

## Run

```bash
cd examples/java/agentic-frameworks-missing-hub
cp kiponos.local.env.example kiponos.local.env   # from kiponos.io → Connect
./gradlew test run
```

## Peers

| Peer | Role |
|------|------|
| Java | this example — `Kiponos.createForCurrentTeam()` |
| Python | `Kiponos.connect()` agent / MCP tool |
| React | `@kiponos/react` **server** peer (`createFromEnv`) — never browser tokens |
| Angular | `@kiponos/angular` **server** peer — same moral |

Medium draft (if any): `docs/examples/medium-drafts/agentic-frameworks-missing-hub.md`  
dev.to essay (if any): `docs/devto-agentic-frameworks-missing-hub.md`
