# agent-budget-token-live

Agent Token Budgets You Can Tighten Mid-Run

Live hub leaf for **live max tokens per turn** — change it without restarting Java, Python agents, React/Angular BFF peers.

## Hub

```text
examples/agent-budget-token-live/max-tokens-per-turn = 4000
```

Pain: runaway agent cost until someone kills the process

## Run

```bash
cd examples/java/agent-budget-token-live
cp kiponos.local.env.example kiponos.local.env   # from kiponos.io → Connect
./gradlew test run
```

## Peers

| Peer | Role |
|------|------|
| Java | this example — `createForCurrentTeam()` |
| Python | `examples/python/agent-budget-token-live` agent sketch |
| React | `@kiponos/react` **server** peer (`createFromEnv`) — never browser tokens |
| Angular | `@kiponos/angular` **server** peer — same moral |

Medium draft: `docs/examples/medium-drafts/agent-budget-token-live.md`  
dev.to essay: `docs/devto-agent-budget-token-live.md`
