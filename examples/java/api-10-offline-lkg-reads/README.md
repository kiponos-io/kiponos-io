# Example api-10 — Offline / last-known-good reads

| | |
|--|--|
| **Level** | Advanced |
| **Pain** | Config server blip kills the app |
| **Fix** | Live vs LKG vs Safe posture around cached reads |

## Run

```bash
export KIPONOS_ID='…'
export KIPONOS_ACCESS='…'
./gradlew test run
```

## Tree

```text
examples / api-10-offline-lkg-reads /
  payment-timeout-ms  = int
```
