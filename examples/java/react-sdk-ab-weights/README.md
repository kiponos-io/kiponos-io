# react-sdk-ab-weights

**A/B Weights You Can Rebalance Mid-Experiment**

Multi-SDK Super Pattern peer set:
- **Java** (this directory) — `./gradlew run` / `./gradlew test`
- **Python** — `examples/python/react-sdk-ab-weights/`
- **Node (react)** — `examples/node/react-sdk-ab-weights/`

## Hub leaf

`experiments/ab-weights` (default `50,50`)

## Run (Java)

```bash
export KIPONOS_ID=… KIPONOS_ACCESS=…
export KIPONOS="['MyApp']['1.0']['Dev']['base']"
./gradlew test
./gradlew run
```

## Story

`docs/examples/medium-drafts/react-sdk-ab-weights.md`
