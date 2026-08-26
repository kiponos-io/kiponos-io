# agentic-metadata-chosen-tree

Agents choose their own shared metadata tree

Live hub leaf for **owner-agent** — change it without restarting Java services, Python agents, or MCP hosts (Grok Build, Cursor, Claude Code).

## Hub

```text
examples/agentic-metadata-chosen-tree/owner-agent = travel-coordinator
```

Pain: Shared state pasted into chat because nobody owned a live folder

## Run

```bash
cd examples/java/agentic-metadata-chosen-tree
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

Medium draft (if any): `docs/examples/medium-drafts/agentic-metadata-chosen-tree.md`  
dev.to essay (if any): `docs/devto-agentic-metadata-chosen-tree.md`
