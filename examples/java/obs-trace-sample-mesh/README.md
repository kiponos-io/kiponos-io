# obs-trace-sample-mesh

Trace Sampling Live Across Servers and Client Mirrors

Live hub leaf for **live trace sample percent** — change it without restarting Java, Python agents, React/Angular BFF peers.

## Hub

```text
examples/obs-trace-sample-mesh/sample-percent = 5
```

Pain: debugging prod means redeploying sample rates or drowning in spans

## Run

```bash
cd examples/java/obs-trace-sample-mesh
cp kiponos.local.env.example kiponos.local.env   # from kiponos.io → Connect
./gradlew test run
```

## Peers

| Peer | Role |
|------|------|
| Java | this example — `createForCurrentTeam()` |
| Python | `examples/python/obs-trace-sample-mesh` agent sketch |
| React | `@kiponos/react` **server** peer (`createFromEnv`) — never browser tokens |
| Angular | `@kiponos/angular` **server** peer — same moral |

Medium draft: `docs/examples/medium-drafts/obs-trace-sample-mesh.md`  
dev.to essay: `docs/devto-obs-trace-sample-mesh.md`
