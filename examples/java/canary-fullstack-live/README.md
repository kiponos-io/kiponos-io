# canary-fullstack-live

Canary Percent That Moves BFF, Workers, and UI Mirrors Together

Live hub leaf for **fullstack canary percent** — change it without restarting Java, Python agents, React/Angular BFF peers.

## Hub

```text
examples/canary-fullstack-live/percent = 0
```

Pain: canaries that only flip one tier while clients keep old behavior

## Run

```bash
cd examples/java/canary-fullstack-live
cp kiponos.local.env.example kiponos.local.env   # from kiponos.io → Connect
./gradlew test run
```

## Peers

| Peer | Role |
|------|------|
| Java | this example — `createForCurrentTeam()` |
| Python | `examples/python/canary-fullstack-live` agent sketch |
| React | `@kiponos/react` **server** peer (`createFromEnv`) — never browser tokens |
| Angular | `@kiponos/angular` **server** peer — same moral |

Medium draft: `docs/examples/medium-drafts/canary-fullstack-live.md`  
dev.to essay: `docs/devto-canary-fullstack-live.md`
