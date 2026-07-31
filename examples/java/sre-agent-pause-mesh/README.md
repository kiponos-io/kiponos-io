# sre-agent-pause-mesh

Pause Heavy Agent Tools When SRE Enters Degradation Mode

Live hub leaf for **agent tool posture under degrade** — change it without restarting Java, Python agents, React/Angular BFF peers.

## Hub

```text
examples/sre-agent-pause-mesh/agent-tools = full
```

Pain: agents keep burning capacity while humans try to stabilize

## Run

```bash
cd examples/java/sre-agent-pause-mesh
cp kiponos.local.env.example kiponos.local.env   # from kiponos.io → Connect
./gradlew test run
```

## Peers

| Peer | Role |
|------|------|
| Java | this example — `createForCurrentTeam()` |
| Python | `examples/python/sre-agent-pause-mesh` agent sketch |
| React | `@kiponos/react` **server** peer (`createFromEnv`) — never browser tokens |
| Angular | `@kiponos/angular` **server** peer — same moral |

Medium draft: `docs/examples/medium-drafts/sre-agent-pause-mesh.md`  
dev.to essay: `docs/devto-sre-agent-pause-mesh.md`
