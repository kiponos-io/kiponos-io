# client-server-mirror-live

Client Mirrors Server Truth — Without Putting Tokens in the Browser

Live hub leaf for **BFF-mirrored status leaf** — change it without restarting Java, Python agents, React/Angular BFF peers.

## Hub

```text
examples/client-server-mirror-live/status = ok
```

Pain: SPA and server disagree because the SPA was treated as a hub peer

## Run

```bash
cd examples/java/client-server-mirror-live
cp kiponos.local.env.example kiponos.local.env   # from kiponos.io → Connect
./gradlew test run
```

## Peers

| Peer | Role |
|------|------|
| Java | this example — `createForCurrentTeam()` |
| Python | `examples/python/client-server-mirror-live` agent sketch |
| React | `@kiponos/react` **server** peer (`createFromEnv`) — never browser tokens |
| Angular | `@kiponos/angular` **server** peer — same moral |

Medium draft: `docs/examples/medium-drafts/client-server-mirror-live.md`  
dev.to essay: `docs/devto-client-server-mirror-live.md`
