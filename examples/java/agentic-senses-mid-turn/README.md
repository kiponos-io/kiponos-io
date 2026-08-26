# agentic-senses-mid-turn

Senses as live hub leaves — mid-turn decisions

Live hub leaf for **priority** — change it without restarting Java services, Python agents, or MCP hosts (Grok Build, Cursor, Claude Code).

## Hub

```text
examples/agentic-senses-mid-turn/priority = P3
```

Pain: Agent finishes a turn on stale network truth because the sense lived in a file

## Run

```bash
cd examples/java/agentic-senses-mid-turn
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

Medium draft (if any): `docs/examples/medium-drafts/agentic-senses-mid-turn.md`  
dev.to essay (if any): `docs/devto-agentic-senses-mid-turn.md`
