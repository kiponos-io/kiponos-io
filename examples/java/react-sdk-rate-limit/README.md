# react-sdk-rate-limit

**Rate Limits the BFF Honors Without Redeploying Node**

Multi-SDK Super Pattern peer set:
- **Java** (this directory) — `./gradlew run` / `./gradlew test`
- **Python** — `examples/python/react-sdk-rate-limit/`
- **Node (react)** — `examples/node/react-sdk-rate-limit/`

## Hub leaf

`limits/rps-cap` (default `40`)

## Run (Java)

```bash
export KIPONOS_ID=… KIPONOS_ACCESS=…
export KIPONOS="['MyApp']['1.0']['Dev']['base']"
./gradlew test
./gradlew run
```

## Story

`docs/examples/medium-drafts/react-sdk-rate-limit.md`
