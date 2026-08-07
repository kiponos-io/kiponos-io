# angular-sdk-ab-weights

**A/B Weights on Angular Without a Client Bundle Ship**

Multi-SDK Super Pattern peer set:
- **Java** (this directory) — `./gradlew run` / `./gradlew test`
- **Python** — `examples/python/angular-sdk-ab-weights/`
- **Node (angular)** — `examples/node/angular-sdk-ab-weights/`

## Hub leaf

`experiments/ab-weights` (default `70,30`)

## Run (Java)

```bash
export KIPONOS_ID=… KIPONOS_ACCESS=…
export KIPONOS="['MyApp']['1.0']['Dev']['base']"
./gradlew test
./gradlew run
```

## Story

`docs/examples/medium-drafts/angular-sdk-ab-weights.md`
