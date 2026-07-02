---
title: "Strangler Fig Migrations With Live Traffic Shift Keys (Kiponos Java SDK)"
published: false
tags: java, architecture, refactoring, devops
description: Gradually replace a legacy monolith by routing feature slices through live Kiponos flags and percentage weights — no big-bang cutover, no redeploy to roll back.
canonical_url: https://dev.to/kiponos/strangler-fig-migrations-with-live-traffic-shift-keys-kiponos-java-sdk-e8p
main_image: https://files.catbox.moe/yath7q.jpg
---

Strangler fig pattern: peel features off a legacy system into new services **incrementally**. The hard part is **routing** — which users hit legacy billing vs new billing? Teams hard-code `if (featureFlag)` branches fed by static config; rollback means **revert + deploy**.

[Kiponos.io](https://kiponos.io) makes strangler routing **operational**:

```java
public BillingPort resolveBilling(String customerId) {
    var mig = kiponos.path("strangler", "billing");
    if (!mig.getBool("migration_enabled")) {
        return legacyBilling;
    }
    int pct = mig.getInt("new_stack_percent");
    if (bucket(customerId) < pct) {
        return newBilling;
    }
    return legacyBilling;
}
```

Increase `new_stack_percent` 5 → 20 → 50 over weeks. Bug in new stack? Set to **0** in dashboard — instant full rollback.

## Strangler tree

```yaml
strangler/
  billing/
    migration_enabled: true
    new_stack_percent: 15
    shadow_compare: true
  catalog/
    migration_enabled: false
    new_stack_percent: 0
  global/
    kill_switch_new_stack: false
```

## Shadow compare mode

`shadow_compare: true` — invoke new stack async, log diffs, **return legacy answer** to user. Tune confidence before raising percent.

## Extreme innovation

Kiponos is the **migration control plane** — product, SRE, and architects share one UI. Legacy and new code paths read same tree; **no duplicate feature-flag vendors** per service.

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — strangler routing you can roll back in one slider.*