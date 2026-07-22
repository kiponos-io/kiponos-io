# Example — Retail checkout timeout

| | |
|--|--|
| **Level** | Intro |
| **Pain** | PSP timeout wrong on Black Friday |
| **Fix** | Live `timeout-ms` + soft-fail flag |

## Run

```bash
export KIPONOS_ID='…'
export KIPONOS_ACCESS='…'
./gradlew test run
```

## Tree

```text
examples / retail-checkout-timeout /
  timeout-ms            = int
  soft-fail-on-timeout  = yes | no
```
