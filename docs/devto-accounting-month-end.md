---
title: "Control Accounting Month-End Close Rules at Runtime (Kiponos Java SDK)"
published: true
tags: java, accounting, enterprise, realtime
description: Adjust reconciliation tolerances, close period gates, and posting rules in Java GL systems without redeploy. Kiponos local reads with live delta updates.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-accounting-month-end.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-accounting-close.jpg
---

Month-end close is a controlled chaos of **tolerance tweaks**, **posting holds**, and **last-minute journal policy**. Finance ops should change those knobs without opening a ticket for a Java redeploy.

[Kiponos.io](https://kiponos.io) exposes close controls as live config:

```java
var close = kiponos.path("accounting", "close");
if (close.getBool("period_locked")) rejectPosting(entry);
if (Math.abs(variance) > close.getDouble("recon_tolerance")) routeToReview(entry);
```

Controllers adjust tolerances in the dashboard; the next journal line sees new values. Static close calendars can remain in ERP — **operational thresholds** live in Kiponos.

Ideal for shared services processing intercompany eliminations across regions.

[kiponos.io](https://kiponos.io) · [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)