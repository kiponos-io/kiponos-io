# angular-sdk-feature-kill

**Feature Kill From the Hub — Angular Peer, Same Leaf as Java**

Multi-SDK Super Pattern peer set:
- **Java** (this directory) — `./gradlew run` / `./gradlew test`
- **Python** — `examples/python/angular-sdk-feature-kill/`
- **Node (angular)** — `examples/node/angular-sdk-feature-kill/`

## Hub leaf

`flags/feature-x` (default `off`)

## Run (Java)

```bash
export KIPONOS_ID=… KIPONOS_ACCESS=…
export KIPONOS="['MyApp']['1.0']['Dev']['base']"
./gradlew test
./gradlew run
```

## Story

`docs/examples/medium-drafts/angular-sdk-feature-kill.md`
