---
title: "Multi-Tenant Kubernetes Namespaces Sharing One Live Kiponos Hub (Java SDK)"
published: true
tags: java, kubernetes, saas, architecture
description: SaaS tenants in separate K8s namespaces connect to tenant-scoped Kiponos profile slices. Platform ops tunes per-tenant limits live — same hub, isolated trees, local reads in every pod.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-multitenant.md
main_image: https://files.catbox.moe/lunm7v.jpg
---

Multi-tenant SaaS on Kubernetes often means **namespace per customer**, **Helm values per tenant**, and a Git repo that does not scale past 50 logos. Platform team needs **per-tenant limits and feature entitlements** that change **today** — not after a values.yaml PR per tenant.

[Kiponos.io](https://kiponos.io) centralizes tenant config in **profile slices** while each tenant's pods run the **same image** with different `-Dkiponos` paths — or shared platform profile with `tenants/{tenantId}/` folders.

## Tenant isolation model

**Option A — profile per tenant:**

```
-Dkiponos="['saas-platform']['v1']['tenant-acme']['live']"
-Dkiponos="['saas-platform']['v1']['tenant-globex']['live']"
```

**Option B — shared profile, tenant subtree:**

```java
var tenant = kiponos.path("tenants", tenantId);
int apiRpm = tenant.path("limits").getInt("api_rpm");
boolean betaUi = tenant.path("features").getBool("beta_dashboard");
```

Both patterns: **local reads** per request, **dashboard edits** per tenant.

## Architecture

![Architecture diagram](https://files.catbox.moe/2m7h8j.png)

Kubernetes RBAC isolates compute; Kiponos ACLs (dashboard) isolate **who edits which tree**.

## Example tenant tree

```yaml
tenants/
  acme/
    limits/
      api_rpm: 5000
      storage_gb: 200
    features/
      beta_dashboard: true
      sso_enabled: true
    billing/
      plan: enterprise
  globex/
    limits/
      api_rpm: 800
    features/
      beta_dashboard: false
platform/
  defaults/
    api_rpm: 600
    max_users: 50
```

## Java: resolve tenant from request context

```java
public TenantPolicy policy(String tenantId) {
    var t = kiponos.path("tenants", tenantId);
    if (!t.exists()) {
        t = kiponos.path("platform", "defaults");
    }
    return new TenantPolicy(
        t.path("limits").getInt("api_rpm"),
        t.path("features").getBool("beta_dashboard")
    );
}
```

Sales upgrades Acme to enterprise → ops bumps `api_rpm` in `tenants/acme` — **no Helm chart fork**, no namespace redeploy.

## Real-world scenarios

| Scenario | Live action |
|----------|-------------|
| Noisy neighbor | Lower one tenant's `api_rpm` |
| Enterprise upsell | Enable `sso_enabled`, raise limits |
| Beta program | Flip `beta_dashboard` for select tenants |
| Platform default change | Edit `platform/defaults` for new signups |

## Performance

Per-tenant reads are still **local cache lookups** — critical when one cluster serves thousands of tenants. WebSocket deltas update only changed tenant subtrees.

## Compare to alternatives

| Approach | Per-tenant live change | Same container image |
|----------|------------------------|----------------------|
| Helm values per tenant | No | Often no |
| CRD per tenant | Heavy | Yes |
| DB tenant config table | Yes | DB on hot path |
| **Kiponos tenant tree** | **Dashboard** | **Yes** |

## Getting started

1. [Free TeamPro at kiponos.io](https://kiponos.io) — namespace folders under `tenants/*`
2. Deploy same image to two namespaces; differ only profile path or tenant routing
3. Change Acme limits in UI; verify Globex pods unchanged

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for multi-tenant Java on Kubernetes. One hub, many tenants, zero per-tenant YAML repos.*