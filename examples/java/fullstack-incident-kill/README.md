# fullstack-incident-kill

Kill the Path Everywhere — JVM, Agent, React BFF, Angular Admin

Live hub leaf for **fullstack path kill flag** — change it without restarting Java, Python agents, React/Angular BFF peers.

## Hub

```text
examples/fullstack-incident-kill/path-enabled = on
```

Pain: kill switches that only hit one tier while the others keep bleeding

## Run

```bash
cd examples/java/fullstack-incident-kill
cp kiponos.local.env.example kiponos.local.env   # from kiponos.io → Connect
./gradlew test run
```

## Peers

| Peer | Role |
|------|------|
| Java | this example — `createForCurrentTeam()` |
| Python | `examples/python/fullstack-incident-kill` agent sketch |
| React | `@kiponos/react` **server** peer (`createFromEnv`) — never browser tokens |
| Angular | `@kiponos/angular` **server** peer — same moral |

Medium draft: `docs/examples/medium-drafts/fullstack-incident-kill.md`  
dev.to essay: `docs/devto-fullstack-incident-kill.md`
