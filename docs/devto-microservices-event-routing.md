---
title: "Retune Event Bus Topic Routing Live Across Microservices (Kiponos Java SDK)"
published: true
tags: java, microservices, events, realtime
description: Change Kafka topic routes, consumer concurrency, and dead-letter policies in Java event services at runtime. Kiponos zero-latency reads.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-microservices-event-routing.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-microservices-events.jpg
---

Event pipelines need **live routing** during incidents — drain a toxic topic, raise consumer parallelism, switch DLQ policy. Java consumers read routing tables from Kiponos:

```java
String topic = kiponos.path("events", "routes", eventType).get("target_topic");
int parallelism = kiponos.path("events", "consumers", group).getInt("parallelism");
```

Platform SRE edits routing in dashboard; consumers see changes on next poll cycle read — local, fast. No consumer group redeploy.

[kiponos.io](https://kiponos.io) · [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)