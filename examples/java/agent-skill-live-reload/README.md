# agent-skill-live-reload

Agent Skills That Reload Without Killing the Session

Live hub leaf for **live skill enable set** — change it without restarting Java, Python agents, React/Angular BFF peers.

## Hub

```text
examples/agent-skill-live-reload/enabled-set = research,notify
```

Pain: agent restarts wipe context just to flip a skill flag

## Run

```bash
cd examples/java/agent-skill-live-reload
cp kiponos.local.env.example kiponos.local.env   # from kiponos.io → Connect
./gradlew test run
```

## Peers

| Peer | Role |
|------|------|
| Java | this example — `createForCurrentTeam()` |
| Python | `examples/python/agent-skill-live-reload` agent sketch |
| React | `@kiponos/react` **server** peer (`createFromEnv`) — never browser tokens |
| Angular | `@kiponos/angular` **server** peer — same moral |

Medium draft: `docs/examples/medium-drafts/agent-skill-live-reload.md`  
dev.to essay: `docs/devto-agent-skill-live-reload.md`
