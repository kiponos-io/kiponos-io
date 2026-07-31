# quad-sdk-live-mesh

Four SDKs, One Living Tree — The Mesh That Does Not Restart

Live hub leaf for **shared mesh posture mode** — change it without restarting Java, Python agents, React/Angular BFF peers.

## Hub

```text
examples/quad-sdk-live-mesh/mode = steady
```

Pain: teams still ship four redeploys to move one operational truth

## Run

```bash
cd examples/java/quad-sdk-live-mesh
cp kiponos.local.env.example kiponos.local.env   # from kiponos.io → Connect
./gradlew test run
```

## Peers

| Peer | Role |
|------|------|
| Java | this example — `createForCurrentTeam()` |
| Python | `examples/python/quad-sdk-live-mesh` agent sketch |
| React | `@kiponos/react` **server** peer (`createFromEnv`) — never browser tokens |
| Angular | `@kiponos/angular` **server** peer — same moral |

Medium draft: `docs/examples/medium-drafts/quad-sdk-live-mesh.md`  
dev.to essay: `docs/devto-quad-sdk-live-mesh.md`
