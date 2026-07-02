---
title: "Retune Event Bus Topic Routing Live Across Java Microservices (Kiponos SDK)"
published: true
tags: java, microservices, eventdriven, realtime
description: Change which topics, consumer groups, and dead-letter routes your Java services use at runtime. Kiponos WebSocket deltas with zero-latency local reads on every publish.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-microservices-event-routing.md
main_image: https://files.catbox.moe/e166i8.jpg
---

Event-driven architectures look elegant in diagrams. In production you are **rerouting traffic** because a consumer is poisoned, a topic is hot, or a new service needs a **shadow subscription** — and every routing change is buried in Spring YAML and a Friday deploy.

[Kiponos.io](https://kiponos.io) lets Java producers and consumers read **live routing tables** from memory: topic names, fan-out targets, DLQ paths, and feature-gated dual-write rules. Ops edits the tree; WebSocket deltas reach every connected SDK; the next `publish()` or `poll()` uses the new route.

## The routing hot path

```java
public void publishOrderEvent(OrderEvent event) {
    String topic = kiponos.path("events", "routing", "orders").get("primary_topic");
    boolean shadowEnabled = kiponos.path("events", "routing", "orders").getBool("shadow_to_analytics");
    kafka.send(topic, event);
    if (shadowEnabled) {
        kafka.send(kiponos.path("events", "routing", "orders").get("shadow_topic"), event);
    }
}
```

If `primary_topic` and `shadow_to_analytics` live in static config, **every routing experiment** is a redeploy. If they live in a remote store you poll per message, you add milliseconds and failure modes to the publish path.

Kiponos: connect once, **local `get()`** on every publish.

## Architecture

![Architecture diagram](https://files.catbox.moe/938yct.png)

## Routing config tree

```yaml
events/
  routing/
    orders/
      primary_topic: orders.v2
      shadow_topic: orders.analytics.shadow
      shadow_to_analytics: true
      dlq_topic: orders.dlq
      max_payload_kb: 256
    payments/
      primary_topic: payments.captured
      route_fraud_alerts: true
      fraud_topic: fraud.signals
    consumers/
      notification-svc/
        group_id: notify-prod-1
        pause_consumption: false
        max_poll_records: 500
```

One tree serves **producers** (which topic to write) and **consumers** (group id, pause flag, batch size).

## Consumer pause without redeploy

```java
@Scheduled(fixedDelay = 1000)
public void pollNotifications() {
    var cfg = kiponos.path("events", "routing", "consumers", "notification-svc");
    if (cfg.getBool("pause_consumption")) {
        return;  // ops flipped pause during downstream outage
    }
    int batch = cfg.getInt("max_poll_records");
    consumer.poll(batch);
}
```

Platform team sets `pause_consumption: true` during an email provider outage — consumers stop pulling **without** scaling pods to zero or editing Kafka ACLs.

## Real-world scenarios

| Scenario | Live action in Kiponos |
|----------|------------------------|
| Poison messages on `orders.v2` | Point `primary_topic` to `orders.v3` |
| Shadow launch new analytics pipeline | Enable `shadow_to_analytics` |
| Consumer lag spike | Lower `max_poll_records` or pause consumer |
| Fraud bridge outage | Set `route_fraud_alerts: false` |

## Performance

- **Local reads** on publish/consume paths — microseconds
- **Delta updates** — toggle shadow routing without reloading entire bus config
- **One WebSocket** per service — not one HTTP call per Kafka message

## Compare to alternatives

| Approach | Change routing mid-day | Per-message read cost |
|----------|------------------------|------------------------|
| Static application.yml | Rolling restart | Zero |
| Kafka topic aliases only | Broker ops | N/A |
| Config server poll | Possible | RTT per poll |
| **Kiponos SDK** | **Dashboard** | **Zero (local)** |

## Getting started

1. [Free TeamPro at kiponos.io](https://kiponos.io) — profile `events/routing/*`
2. Mirror your topic map into the Kiponos tree
3. Replace hard-coded topic strings with `kiponos.path("events", "routing", ...).get(...)`
4. Enable shadow routing in UI; confirm dual-write without redeploy

Golden Java example: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

## What is next

Routing pairs naturally with **saga timeouts** and **cross-service handoff flags** — operational control for event-driven systems in one live hub.

---

*Kiponos.io — real-time config for Java. Steer your event bus while messages are flowing.*