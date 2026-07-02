---
title: "Canary Releases Without a Service Mesh — Live Traffic Weights in Kiponos (Java SDK)"
published: false
tags: java, architecture, deployment, devops
description: Shift traffic between v1 and v2 using live weight keys read in your Java router — no Istio, no second deployment mid-incident. Kiponos zero-latency reads.
canonical_url: https://dev.to/kiponos/canary-releases-without-a-service-mesh-live-traffic-weights-in-kiponos-java-sdk-3i38
main_image: https://files.catbox.moe/68lli5.jpg
---

Canary releases usually mean **mesh config**, **duplicate Deployments**, or **load balancer Terraform** — slow loops when v2 shows errors and you need **0% → rollback NOW**.

[Kiponos.io](https://kiponos.io) enables **application-level canary** when your Java edge/router chooses backends:

```java
public String pickBackend(String userId) {
    var canary = kiponos.path("release", "canary");
    int v2Weight = canary.getInt("v2_weight");  // 0-100
    int roll = Math.floorMod(userId.hashCode(), 100);
    return roll < v2Weight ? "payments-v2" : "payments-v1";
}
```

SRE slides `v2_weight` 5 → 25 → 50 in dashboard — **seconds**, not pipeline.

## Canary tree

```yaml
release/
  canary/
    v2_weight: 10
    enabled: true
    sticky_by_user: true
    error_budget_pause: false
  backends/
    v1_url: http://payments-v1.svc
    v2_url: http://payments-v2.svc
```

## Extreme: auto rollback hook

```java
kiponos.afterValueChanged(change -> {
    if ("release/canary/error_budget_pause".equals(change.path()) && change.newValue().equals(true)) {
        kiponos.path("release", "canary").set("v2_weight", 0);
    }
});
```

Monitoring service sets `error_budget_pause` — weight zeroes without human clicking.

## Compare

| Approach | Speed of rollback | Infra complexity |
|----------|-------------------|------------------|
| Mesh VirtualService | Minutes | High |
| Second LB rule | Terraform | Medium |
| **Kiponos weights** | **Dashboard** | **Router code + hub** |

Related: [A/B checkout weights](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-ab-checkout-weights.md)

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — canary by live weight, not by emergency Terraform.*