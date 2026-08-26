# agentic-travel-group-chat-live

Travel Coordinator App — live group-chat mute via Kiponos

Live hub leaf for **chat-mute** — mute write-tools on a noisy channel without killing the agent session or rebooting MCP.

## Hub

```text
examples/agentic-travel-group-chat-live/chat-mute = none
```

Pain: delayed flight floods a group chat; agents keep posting while ops needs silence on one channel

## Run

```bash
cd examples/java/agentic-travel-group-chat-live
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

dev.to essay: `docs/devto-agentic-travel-group-chat-live.md`
