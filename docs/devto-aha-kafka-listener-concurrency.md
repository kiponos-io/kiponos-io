---
main_image: 
title: "spring.kafka.listener.concurrency Was Frozen at Boot — We Scaled Consumer Threads Live During a Backlog"
published: false
tags: java, kafka, springboot, devops
description: Spring Kafka listener concurrency is often fixed at container start. When lag spikes, Kiponos lets you raise (or lower) concurrency without a redeploy — carefully, with partition awareness.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-kafka-listener-concurrency.md
---

Friday 16:55. Lag on `orders.created` is **2.1M**. Consumer group has `concurrency: 3` because that was "safe" in January. Partitions are 12. You have headroom. You do not have a release train.

```yaml
spring:
  kafka:
    listener:
      concurrency: 3
```

> "Concurrency is **topology**. You do not hot-change consumer threads."

**The Aha:** listener concurrency (within partition limits) is **lag policy**. [Kiponos.io](https://kiponos.io) holds a live integer; you stop/start the concurrent message listener container or adjust concurrency on the container when the value changes — local get, dashboard delta.

## Belief vs production

| Belief | Reality |
|--------|---------|
| concurrency=3 forever | Peak weeks need 8; nights need 2 |
| More threads always help | Past partition count they idle / fight |
| max.poll.* is enough | Poll and concurrency are different levers |

Pair with [kafka max.poll](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-kafka-max-poll.md) so you do not raise concurrency while records-per-poll still thrash rebalances.

## Kiponos shape

```yaml
kafka_ops/
  consumers/
    orders_created/
      concurrency: 3
      max_concurrency: 12
      min_concurrency: 1
```

## Integration sketch

```java
int n = kiponos.path("kafka_ops", "consumers", "orders_created")
    .getInt("concurrency", 3);
// ConcurrentMessageListenerContainer#setConcurrency(n) then stop/start
// Guard: n <= partition count and <= max_concurrency
```

## Scenarios

| Moment | Frozen | Live |
|--------|--------|------|
| Flash sale lag | Page + hope | Raise to 10 under max |
| Quiet night | Waste CPU | Drop to 2 |
| Poison storm | Scale thrash | Lower + open circuit elsewhere |

## Before / after

| Approach | Mid-backlog | Risk |
|----------|-------------|------|
| Redeploy | Slow | Queue grows |
| Manual kubectl scale pods | Coarse | More pods ≠ more partitions used well |
| **Live concurrency** | **Fast** | Must respect partitions |

## When not

| Case | Prefer |
|------|--------|
| Need more partitions | Topic redesign |
| Exactly-once re-architecture | Code + careful rollout |
| Shared consumer group multi-app | Ownership first |

## Getting started

1. Put concurrency + min/max guards in hub  
2. Wire container refresh on delta  
3. Alert if concurrency > partition count  
4. Document which consumer owns which topic  

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io) · [GETTING-STARTED](https://github.com/kiponos-io/kiponos-io/blob/master/GETTING-STARTED.md)

---

*Kiponos.io — consumer concurrency is lag policy, not a January guess.*
