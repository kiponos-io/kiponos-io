# The knob was trivial — the redeploy was not: `timeout-ms`

*Traveler note: read http client timeout live.*

## Hub

```text
examples / aha-http-timeout / timeout-ms = 3000
```

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/aha-http-timeout
./gradlew test run
```

## Moral

People should not have to ship a release to make a decision.

---
*Example: https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-http-timeout*
