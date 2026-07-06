---
title: "Change Kubernetes Config Without Pod Restart or Redeploy (Kiponos Java SDK)"
published: true
tags: java, kubernetes, devops, realtime
description: ConfigMap changes usually mean rolling restarts. Kiponos WebSocket deltas update in-memory config inside running pods — tune Java services during incidents without touching Deployments.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-no-restart.md
main_image: https://files.catbox.moe/vkgtrb.jpg
---

The default Kubernetes config story ends with **`kubectl rollout restart`**. You fixed a rate limit in a ConfigMap; now you are waiting for twelve pods to cycle during peak traffic. Java services with Spring refresh scopes help for some beans — not for everything, and not without engineering ceremony.

[Kiponos.io](https://kiponos.io) updates **running JVMs** through WebSocket delta patches into the SDK's in-memory tree. Ops edits the dashboard; connected pods see new values on the next `kiponos.path(...).get*()` — **no Deployment spec change, no image rebuild, no graceful drain**.

## Restart vs live update

| Change type | ConfigMap model | Kiponos model |
|-------------|-----------------|---------------|
| Fraud threshold | Rolling restart | Dashboard edit |
| Feature flag | Reloader + restart | Delta patch |
| JDBC URL (emergency) | Restart + risk | Live update + connection factory hook |
| Rate limit per tenant | New Helm release | Instant local read |

## How it works inside the pod

![Architecture diagram](https://files.catbox.moe/73lfeg.png)

The pod process **never exits**. Kubernetes is unaware config changed — by design.

## Java: react to changes optionally

Hot path stays read-only:

```java
int rpm = kiponos.path("limits", tenantId).getInt("rpm");
```

For resources that need refresh (connection pools, cached clients):

```java
kiponos.afterValueChanged(change -> {
    if (change.path().startsWith("data/postgres")) {
        dataSource.resizePool(kiponos.path("data", "postgres").getInt("pool_max"));
    }
});
```

Keep listeners **lightweight** — heavy work async; reads stay local.

## Incident playbook example

1. Alert: downstream payment processor elevated latency
2. Ops increases `payment/client/timeout_ms` and enables `degraded_mode` in Kiponos
3. All payment pods pick up values within WebSocket RTT
4. **Zero** pods restarted; HPA does not flap

## Interaction with HPA and rollouts

| Concern | Note |
|---------|------|
| HPA scaling | New pods connect to Kiponos; receive full snapshot + deltas |
| Blue/green deploy | New revision still uses same profile — config independent of rollout |
| Pod crash | Replacement pod loads latest tree on connect |

Config lifecycle **decouples** from Deployment lifecycle.

## Real-world scenarios

| Scenario | Without restart |
|----------|-----------------|
| Circuit breaker tuning | Edit `breakers/*` keys |
| Kill switch feature | `features/new_checkout_enabled: false` |
| Throttle abusive tenant | Lower per-tenant `rpm` |
| Ops drill | Flip `degraded_mode` cluster-wide |

## Performance

Identical to non-K8s Java: **local O(1) reads**, **async WebSocket worker**. Rolling restart cost (GC warmup, cache cold start) is avoided.

## Compare to alternatives

| Approach | Avoids rollout | Works on every tunable knob |
|----------|----------------|----------------------------|
| ConfigMap alone | No | N/A |
| Spring Cloud Bus refresh | Partial | Bean-dependent |
| Sidecar config sync | Sometimes | Complex |
| **Kiponos SDK** | **Yes** | **Any key you read via SDK** |

## Getting started

1. Deploy Java service with Kiponos SDK ([no ConfigMap pattern](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-no-configmaps.md))
2. `kubectl get pods` — note running pod names
3. Change a live flag in dashboard
4. Hit API — behavior changes; **same pod age** (`kubectl get pods` START TIME unchanged)

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for Java on Kubernetes. Tune running pods, not Deployment YAML.*