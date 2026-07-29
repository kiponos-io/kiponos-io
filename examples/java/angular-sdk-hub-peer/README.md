# angular-sdk-hub-peer

Java peer that **reads** a live `demo/status-wall/*` tree written by a Node
`@kiponos/angular` `createFromEnv` service (and the dashboard / React peers).

## Point

Same hub participant model: `Kiponos.createForCurrentTeam()`, local `get()`,
live deltas — no redeploy when an Angular BFF or status service flips a key.

## Public packages

| Package | Tree |
|---------|------|
| `@kiponos/angular` | [`sdks/kiponos-angular-sdk`](../../../sdks/kiponos-angular-sdk) |
| `@kiponos/react` | [`sdks/kiponos-react-sdk`](../../../sdks/kiponos-react-sdk) |

## Run

```bash
export KIPONOS_ID=… KIPONOS_ACCESS=…
export KIPONOS="['MyApp']['1.0']['Dev']['base']"
# ./gradlew run   # when wired like sibling examples
```

See draft: `docs/examples/medium-drafts/angular-sdk-hub-peer.md`

Do **not** link private team homes from public docs.
