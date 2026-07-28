# react-sdk-hub-peer

Java peer that **reads** a live `demo/status-wall/*` tree written by a Node
`@kiponos/react` `createFromEnv` status-wall service (and the dashboard).

## Point

Same hub participant model as other examples: `Kiponos.createForCurrentTeam()`,
local `get()`, live deltas — no redeploy when the Node wall flips a status.

## Run

```bash
export KIPONOS_ID=… KIPONOS_ACCESS=…
export KIPONOS="['MyApp']['1.0']['Dev']['base']"  # use your own profile
# ./gradlew run   # when wired like sibling examples
```

See draft: `docs/examples/medium-drafts/react-sdk-hub-peer.md`  

Do **not** link private team homes (ops walls, family tools) from public docs.
