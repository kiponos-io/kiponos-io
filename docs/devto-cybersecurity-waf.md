---
title: "Live WAF and Security Rule Tuning — No Java Restart (Kiponos SDK)"
published: true
tags: java, security, devops, realtime
description: Update WAF sensitivity, IP block thresholds, and bot scores in Java security gateways at runtime. Zero-latency Kiponos reads per request.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-cybersecurity-waf.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-waf.jpg
---

During an attack, security teams tighten WAF rules **now**. During false-positive storms, they loosen them **now**. Java gateways read live policy:

```java
int botThreshold = kiponos.path("waf", "bot").getInt("block_score");
boolean strictMode = kiponos.path("waf", "modes").getBool("strict");
```

SOC edits thresholds in Kiponos; next HTTP evaluation uses new values. No gateway pod restart mid-incident.

[kiponos.io](https://kiponos.io) · [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)