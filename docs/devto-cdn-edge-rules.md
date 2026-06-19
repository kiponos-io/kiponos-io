---
title: "Tune CDN Edge Cache and Routing Rules at Runtime (Kiponos Java SDK)"
published: true
tags: java, devops, cdn, realtime
description: Change TTLs, geo routing, and origin failover weights on Java CDN edge controllers without redeploy. Kiponos local reads on every request.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-cdn-edge-rules.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-cdn.jpg
---

Origin meltdown? Extend TTLs. Launch in APAC? Shift geo weights. Java edge nodes read live rules:

```java
int ttl = kiponos.path("cdn", "cache").getInt("default_ttl_sec");
String origin = kiponos.path("cdn", "origins").get("primary");
```

Ops edits one dashboard value; global edge fleet receives delta over WebSocket. No edge config bundle rollout.

[kiponos.io](https://kiponos.io) · [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)