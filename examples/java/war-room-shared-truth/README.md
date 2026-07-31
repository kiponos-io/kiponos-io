# war-room-shared-truth

One War-Room Wall: Humans, Agents, and SDKs on the Same Leaf

Live hub leaf for **shared war-room headline** — change it without restarting Java, Python agents, React/Angular BFF peers.

## Hub

```text
examples/war-room-shared-truth/headline = steady
```

Pain: ops walls, agent state, and service status disagree mid-incident

## Run

```bash
cd examples/java/war-room-shared-truth
cp kiponos.local.env.example kiponos.local.env   # from kiponos.io → Connect
./gradlew test run
```

## Peers

| Peer | Role |
|------|------|
| Java | this example — `createForCurrentTeam()` |
| Python | `examples/python/war-room-shared-truth` agent sketch |
| React | `@kiponos/react` **server** peer (`createFromEnv`) — never browser tokens |
| Angular | `@kiponos/angular` **server** peer — same moral |

Medium draft: `docs/examples/medium-drafts/war-room-shared-truth.md`  
dev.to essay: `docs/devto-war-room-shared-truth.md`
