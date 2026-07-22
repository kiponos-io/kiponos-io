# Example 16 — Kafka consumer worker knobs

| | |
|--|--|
| **Level** | Core |
| **Pain** | Pause / prefetch / max-poll only via redeploy |
| **Fix** | Live tree under `examples/16-kafka-consumer-worker` |

## Run

```bash
export KIPONOS_ID='…'
export KIPONOS_ACCESS='…'
./gradlew test run
```

## Tree

```text
examples / 16-kafka-consumer-worker /
  paused            = yes | no
  prefetch          = int
  max-poll-records  = int
```
