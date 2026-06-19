---
title: "Orchestrate Saga Compensation Timeouts in Real Time (Kiponos Java SDK)"
published: true
tags: java, microservices, distributed, realtime
description: Tune saga step timeouts, retry budgets, and compensation triggers across Java services without redeploy. Shared Kiponos tree with local reads.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-microservices-saga.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-microservices-saga.jpg
---

Distributed sagas need **tunable** timeouts — downstream slow? Extend wait. Partner outage? Shorten and compensate faster. Hard-coded saga YAML cannot keep up.

```java
int waitMs = kiponos.path("sagas", "checkout", "payment").getInt("step_timeout_ms");
int retries = kiponos.path("sagas", "checkout", "payment").getInt("max_retries");
```

Every saga participant reads the same live tree. Platform ops adjusts `step_timeout_ms` once; **all JVMs** pick it up via WebSocket delta. No redeploy across twelve services.

[kiponos.io](https://kiponos.io) · [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)