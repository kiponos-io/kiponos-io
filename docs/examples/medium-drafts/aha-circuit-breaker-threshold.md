# The knob was trivial — the redeploy was not: `failure-threshold`

*Traveler note: circuit breaker threshold live.*

## Hub

```text
examples / aha-circuit-breaker-threshold / failure-threshold = 5
```

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/aha-circuit-breaker-threshold
./gradlew test run
```

## Moral

People should not have to ship a release to make a decision.

---
*Example: https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-circuit-breaker-threshold*
