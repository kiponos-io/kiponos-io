---
main_image: 
title: "spring.transaction.default-timeout Was a Footgun — We Changed Transaction Timeouts Live Without Restarting Pods"
published: false
tags: java, springboot, database, devops
description: Spring default transaction timeouts often freeze at startup. When a report query holds rows too long, Kiponos lets you tighten transaction ceilings live so the rest of the app can breathe.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-tx-default-timeout.md
---

Monday 09:52. A "small" admin report opens a `@Transactional` method and holds a connection for **four minutes**. Cart writers start waiting on locks. Hikari looks fine until it does not.

Your global ceiling:

```properties
spring.transaction.default-timeout=30
```

…was never set. Or it was set to `300` "just in case." Either way, changing it mid-day is a redeploy conversation.

> "Transaction timeout is a **data-layer contract**. We do not flip that live."

**The Aha:** default (and per-use-case) transaction timeouts are **operational ceilings**, not schema. [Kiponos.io](https://kiponos.io) holds them live; you apply on `PlatformTransactionManager` / programmatic transactions from local gets.

## Belief vs production

| Belief | Reality |
|--------|---------|
| Report jobs can take "as long as needed" | They steal locks from checkout |
| Default 30s is enough forever | Nightly jobs and flash sales need different ceilings |
| Annotation value is sacred | Annotation can read a live supplier |

## Kiponos shape

```yaml
db_ops/
  tx/
    default_timeout_sec: 30
    report_timeout_sec: 120
    checkout_timeout_sec: 5
```

Pair with [JDBC statement_timeout](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-jdbc-statement-timeout.md) — transaction timeout and statement timeout are **siblings**, not substitutes.

## Integration sketch

```java
int sec = kiponos.path("db_ops", "tx").getInt("checkout_timeout_sec", 5);
DefaultTransactionDefinition def = new DefaultTransactionDefinition();
def.setTimeout(sec);
// or TransactionTemplate with live timeout before each execute
```

## Scenarios

| Moment | Frozen | Live |
|--------|--------|------|
| Lock pile-up | Kill query manually | Drop default to 10s |
| Month-end export | Hope | Open `report_timeout_sec` only |
| Flash sale | Guess | Tighten checkout only |

## Before / after

| Approach | Mid-incident | Hot path |
|----------|--------------|----------|
| properties + restart | Slow | Frozen |
| DB session GUCs only | Partial | Needs connection checkout hooks |
| **Kiponos + TX API** | **Seconds** | **Local get** |

## When not

| Case | Prefer |
|------|--------|
| Isolation level redesign | Code + review |
| Schema migration windows | Change management |
| Long batch by design | Dedicated batch pool + explicit timeouts |

## Getting started

1. Define default / checkout / report timeout ints in hub  
2. Programmatic TX or aspect reads local tree  
3. Game day: hold a lock; prove ceiling trips  
4. Align with statement_timeout so both fire predictably  

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io) · [GETTING-STARTED](https://github.com/kiponos-io/kiponos-io/blob/master/GETTING-STARTED.md)

---

*Kiponos.io — transaction ceilings protect the whole app, not just the slow method.*
