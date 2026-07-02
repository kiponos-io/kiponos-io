---
title: "Tune JDBC and HTTP Connection Pools at Runtime (Kiponos Java SDK)"
published: false
tags: java, architecture, performance, devops
description: Pool sizes and timeouts baked into HikariCP config cannot react to DB failover or traffic spikes. Kiponos feeds live pool parameters — resize on afterValueChanged without pod restart.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-connection-pool-live.md
main_image: https://files.catbox.moe/ffnuj2.jpg
---

Connection pool exhaustion pages people at the worst moment. You want **lower maxPoolSize** to protect a struggling database, or **raise** it when read replicas come online. Hikari `maximumPoolSize` in YAML means **restart every JVM**.

[Kiponos.io](https://kiponos.io) stores pool knobs live:

```java
@PostConstruct
void bindPool() {
    kiponos.afterValueChanged(this::maybeResizePool);
}

void maybeResizePool(ValueChange change) {
    if (!change.path().startsWith("pools/postgres")) return;
    int max = kiponos.path("pools", "postgres").getInt("max_size");
    dataSource.setMaximumPoolSize(max);
}
```

Hot path still borrows connections normally — resize is **async** on config change.

## Pool tree

```yaml
pools/
  postgres/
    max_size: 20
    min_idle: 5
    connection_timeout_ms: 3000
  http_client/
    max_per_route: 50
    max_total: 200
```

## Scenarios

| Event | Live tweak |
|-------|------------|
| DB CPU pegged | Lower `max_size` cluster-wide |
| Replica promotion | Point JDBC URL + raise pool |
| Partner rate limit | Lower `http_client/max_per_route` |

Pair with [K8s no restart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-no-restart.md).

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — pools that breathe with the database.*