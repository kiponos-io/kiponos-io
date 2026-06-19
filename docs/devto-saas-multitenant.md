---
title: "SaaS Multi-Tenant Feature Entitlements in Real Time (Kiponos Java SDK)"
published: true
tags: java, saas, multitenancy, realtime
description: Toggle tenant features, seat limits, and plan gates in Java SaaS apps without redeploy. Per-tenant folders in Kiponos with zero-latency reads.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-saas-multitenant.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-saas.jpg
---

Enterprise CS needs to **enable a beta feature** for one tenant now — not after tonight's deploy. Java SaaS services read entitlements from Kiponos:

```java
var tenant = kiponos.path("tenants", tenantId);
if (!tenant.getBool("feature_analytics_v2")) return forbidden();
if (seatsUsed > tenant.getInt("seat_cap")) return upgradeRequired();
```

Support edits tenant folder in UI; next API call enforces new gates. Same pattern scales to thousands of tenants via profile structure.

[kiponos.io](https://kiponos.io) · [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)