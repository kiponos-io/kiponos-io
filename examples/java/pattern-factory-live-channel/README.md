# Super Pattern: Live Factory Method (Notify Channel)

**Gang of Four:** Factory Method  
**Kiponos Super Pattern:** which product to create is a hub value.

## Problem

`createNotifier()` is a switch on env or constants. Moving from email to Slack during an incident is a deploy.

## Super Pattern

```text
patterns / factory / notify / product    = email | sms | push | slack
patterns / factory / notify / from-email = string
patterns / factory / notify / slack-hook = string
```

## Run

```bash
cd examples/java/pattern-factory-live-channel
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
./gradlew run --args='Warehouse delay on order 9'
```

## Python parity

`examples/python/pattern-factory-live-channel/`

## Moral

**Factories create objects. Super factories create the *right* object for the next minute of production.**
