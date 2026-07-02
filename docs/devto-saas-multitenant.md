---
title: "SaaS Multi-Tenant Feature Entitlements in Real Time (Kiponos Java SDK)"
published: true
tags: java, saas, architecture, realtime
description: Per-tenant feature flags, seat limits, and API quotas in Java SaaS apps — live Kiponos tree with local reads. Sales upgrades a tenant without redeploying Helm values.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-saas-multitenant.md
main_image: https://files.catbox.moe/z65ex6.jpg
---

Multi-tenant SaaS ships the **same JAR** to every customer. Differentiation lives in **entitlements**: API rate limits, seat caps, beta features, storage quotas. Sales closes an enterprise deal at 4 PM; engineering should not fork `values-acme.yaml` and wait for a cluster rollout.

[Kiponos.io](https://kiponos.io) models entitlements as `tenants/{tenantId}/features` and `tenants/{tenantId}/limits` — every request resolves policy with **local** `kiponos.path(...).get*()` calls.

## Request gate

```java
public void enforceEntitlements(String tenantId, User user, String feature) {
    var t = kiponos.path("tenants", tenantId);
    if (!t.path("features").getBool(feature)) {
        throw new ForbiddenException("not_entitled");
    }
    if (activeSeats(tenantId) >= t.path("limits").getInt("max_seats")) {
        throw new LimitException("seat_cap");
    }
}
```

## Tenant tree

```yaml
tenants/
  acme/
    features/
      sso: true
      beta_analytics: true
      api_v2: true
    limits/
      max_seats: 500
      api_rpm: 10000
      storage_gb: 2000
  startup-42/
    features/
      sso: false
      beta_analytics: false
    limits/
      max_seats: 25
      api_rpm: 600
platform/
  defaults/
    max_seats: 10
    api_rpm: 120
```

## Real-world scenarios

| Scenario | Live action |
|----------|-------------|
| Enterprise upsell | Enable `sso`, raise `max_seats` |
| Abuse mitigation | Lower `api_rpm` for one tenant |
| Gradual beta | Flip `beta_analytics` per logo |
| Trial expiry | Disable features via dashboard |

## Performance

Entitlement checks run **per API call** — must be O(1) local reads.

## Getting started

1. [kiponos.io](https://kiponos.io) — `tenants/*`
2. Replace hard-coded plan maps with Kiponos folders
3. Upgrade test tenant live; hit API — new limits apply immediately

See also: [K8s multi-tenant namespaces](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-multitenant.md)

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for Java SaaS. Entitlements when sales closes — not when Helm merges.*