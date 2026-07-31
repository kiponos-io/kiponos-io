# agent-debug-probe-live

Production Debug Probes for Agents and Services — While They Run

Live hub leaf for **live verbose probe flag** — change it without restarting Java, Python agents, React/Angular BFF peers.

## Hub

```text
examples/agent-debug-probe-live/verbose = off
```

Pain: verbose agent logs only after a restart that changes the bug

## Run

```bash
cd examples/java/agent-debug-probe-live
cp kiponos.local.env.example kiponos.local.env   # from kiponos.io → Connect
./gradlew test run
```

## Peers

| Peer | Role |
|------|------|
| Java | this example — `createForCurrentTeam()` |
| Python | `examples/python/agent-debug-probe-live` agent sketch |
| React | `@kiponos/react` **server** peer (`createFromEnv`) — never browser tokens |
| Angular | `@kiponos/angular` **server** peer — same moral |

Medium draft: `docs/examples/medium-drafts/agent-debug-probe-live.md`  
dev.to essay: `docs/devto-agent-debug-probe-live.md`
