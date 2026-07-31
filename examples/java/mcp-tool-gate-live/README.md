# mcp-tool-gate-live

MCP Tool Gates Without Restarting the Agent Host

Live hub leaf for **live MCP tool allow-list** — change it without restarting Java, Python agents, React/Angular BFF peers.

## Hub

```text
examples/mcp-tool-gate-live/tools-allow = read,search
```

Pain: dangerous tools stay hot until someone restarts the MCP host

## Run

```bash
cd examples/java/mcp-tool-gate-live
cp kiponos.local.env.example kiponos.local.env   # from kiponos.io → Connect
./gradlew test run
```

## Peers

| Peer | Role |
|------|------|
| Java | this example — `createForCurrentTeam()` |
| Python | `examples/python/mcp-tool-gate-live` agent sketch |
| React | `@kiponos/react` **server** peer (`createFromEnv`) — never browser tokens |
| Angular | `@kiponos/angular` **server** peer — same moral |

Medium draft: `docs/examples/medium-drafts/mcp-tool-gate-live.md`  
dev.to essay: `docs/devto-mcp-tool-gate-live.md`
