---
title: "Kubernetes Pods Without ConfigMaps or Environment Variables (Kiponos Java SDK)"
published: true
tags: java, kubernetes, devops, realtime
description: Remove ConfigMaps, envFrom walls, and volume-mounted YAML from pods. Each container reads live config from the Kiponos SDK — delta updates without reload operators or rolling restarts.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-no-configmaps.md
main_image: https://files.catbox.moe/geqada.jpg
---

Kubernetes taught us to inject config through ConfigMaps, Secrets-as-files, `envFrom`, and Reloader annotations that trigger **rolling restarts** when someone fixes a typo in `application.yml`. For Java services that need **mid-day tuning**, that model is backwards.

[Kiponos.io](https://kiponos.io) inverts pod configuration: mount **only auth tokens**, let the SDK hold operational config **in memory**, updated via WebSocket deltas. No ConfigMap volume. No `DATABASE_URL` env var matrix. No Stakater Reloader.

## What stays in Kubernetes vs Kiponos

| Kubernetes (secrets) | Kiponos (live hub) |
|----------------------|-------------------|
| `KIPONOS_ID` JWE token | JDBC URLs, pool sizes |
| `KIPONOS_ACCESS` JWE token | Feature flags, rate limits |
| TLS certs, IAM keys | Partner endpoints, timeouts |
| | Anything ops changes weekly |

Secrets stay in K8s; **behavioral config** moves to Kiponos.

## Minimal pod spec

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payments-api
spec:
  template:
    spec:
      containers:
        - name: app
          image: payments-api:1.4.2
          env:
            - name: KIPONOS_ID
              valueFrom:
                secretKeyRef:
                  name: kiponos-auth
                  key: id
            - name: KIPONOS_ACCESS
              valueFrom:
                secretKeyRef:
                  name: kiponos-auth
                  key: access
          args:
            - "-Dkiponos=['payments']['v3']['prod']['live']"
          # No configMapRef. No envFrom. No application.yml volume.
```

## Application code

```java
@Service
public class DataSourceFactory {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();

    public HikariConfig config() {
        var db = kiponos.path("data", "postgres");
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(db.get("jdbc_url"));
        cfg.setMaximumPoolSize(db.getInt("pool_max"));
        return cfg;
    }
}
```

Pool size changes in dashboard → SDK receives delta → **next** `getInt("pool_max")` returns new value. Implement pool resize in your factory or on `afterValueChanged` — without a new Deployment.

## Architecture

![Architecture diagram](https://files.catbox.moe/to4cqn.png)

## Why teams delete ConfigMaps

| ConfigMap pain | Kiponos answer |
|----------------|----------------|
| 1MB size limits | Tree in hub, not etcd |
| Restart to pick up changes | WebSocket deltas |
| Per-env duplicate manifests | Profile path per env |
| Drift between Helm charts | Single dashboard truth |

## Real-world scenarios

| Scenario | Action |
|----------|--------|
| Point staging at new DB | Edit `data/postgres/jdbc_url` in staging profile |
| Reduce pool during incident | Lower `pool_max` live |
| Enable read replica routing | Flip `read_replica_enabled` without new image |
| Black Friday | Bump connection and timeout knobs from dashboard |

## Performance

- **One WebSocket per pod** — not ConfigMap watch + file reload per key
- **Reads are local** — same as bare-metal Java services
- **Delta patches** — one changed URL does not remount volumes

## Compare to alternatives

| Approach | Live change without rollout | Pod spec complexity |
|----------|----------------------------|---------------------|
| ConfigMap + reload | Usually requires restart | High |
| External Secrets Operator | Sync lag | High |
| Spring Cloud Config poll | Polling latency | Medium |
| **Kiponos SDK in pod** | **Dashboard delta** | **Two env vars + JVM arg** |

## Getting started

1. [Free TeamPro at kiponos.io](https://kiponos.io) — profile per env (`prod`, `staging`)
2. Create `kiponos-auth` Secret with team tokens
3. Remove ConfigMap volumes from Deployment; add SDK dependency
4. `kubectl apply`; change a flag in UI; verify pod behavior updates without rollout

See also: [config changes without pod restart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-no-restart.md)

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for Java on Kubernetes. Pods carry tokens, not ConfigMaps.*