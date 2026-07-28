# react-sdk-hub-peer

Java peer that **reads** the live `family/mirror-phone/*` tree written by the Node
`@kiponos/react` `createFromEnv` Mirror Phone service (and the dashboard).

## Point

Same hub participant model as other examples: `Kiponos.createForCurrentTeam()`,
local `get()`, live deltas — no redeploy when Mirror Phone flips a status.

## Run

```bash
export KIPONOS_ID=… KIPONOS_ACCESS=…
export KIPONOS="['Family-Agent']['1.0.0']['Alef-Dev']['base']"  # or your profile
# ./gradlew run   # when wired like sibling examples
```

See draft: `docs/examples/medium-drafts/react-sdk-hub-peer.md`  
Live wall: https://kiponos.io/mirror/
