# multi-agent-collab-bus

Agents That Hand Off Work Without a Restart Handshake

Live hub leaf for **live agent handoff ticket** — change it without restarting Java, Python agents, React/Angular BFF peers.

## Hub

```text
examples/multi-agent-collab-bus/handoff-ticket = idle
```

Pain: multi-agent pipelines freeze on process restarts and file drops

## Run

```bash
cd examples/java/multi-agent-collab-bus
cp kiponos.local.env.example kiponos.local.env   # from kiponos.io → Connect
./gradlew test run
```

## Peers

| Peer | Role |
|------|------|
| Java | this example — `createForCurrentTeam()` |
| Python | `examples/python/multi-agent-collab-bus` agent sketch |
| React | `@kiponos/react` **server** peer (`createFromEnv`) — never browser tokens |
| Angular | `@kiponos/angular` **server** peer — same moral |

Medium draft: `docs/examples/medium-drafts/multi-agent-collab-bus.md`  
dev.to essay: `docs/devto-multi-agent-collab-bus.md`
