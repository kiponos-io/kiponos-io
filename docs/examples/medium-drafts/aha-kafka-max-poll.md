# The knob was trivial — the redeploy was not: `max-poll-records`

*Traveler note: kafka max poll records live.*

## Hub

```text
examples / aha-kafka-max-poll / max-poll-records = 50
```

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/aha-kafka-max-poll
./gradlew test run
```

## Moral

People should not have to ship a release to make a decision.

---
*Example: https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-kafka-max-poll*
