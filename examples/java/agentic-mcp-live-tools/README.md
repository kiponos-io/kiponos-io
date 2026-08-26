# agentic-mcp-live-tools

MCP tools that read a live hub — no host restart

Live hub leaf for **tools-allow** — change it without restarting Java services, Python agents, or MCP hosts (Grok Build, Cursor, Claude Code).

## Hub

```text
examples/agentic-mcp-live-tools/tools-allow = search,read
```

Pain: Restarting Grok Build / Cursor / Claude Code MCP just to flip a write tool

## Run

```bash
cd examples/java/agentic-mcp-live-tools
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

Medium draft (if any): `docs/examples/medium-drafts/agentic-mcp-live-tools.md`  
dev.to essay (if any): `docs/devto-agentic-mcp-live-tools.md`
