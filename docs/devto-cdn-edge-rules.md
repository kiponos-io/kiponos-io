---
title: "Tune CDN Edge Cache and Routing Rules at Runtime (Kiponos Java SDK)"
published: true
tags: java, cdn, devops, realtime
description: Change cache TTLs, origin weights, and geo routing rules in Java CDN control services without edge config pushes. Kiponos local reads, WebSocket deltas.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-cdn-edge-rules.md
main_image: https://files.catbox.moe/i8rg0m.jpg
---

CDN incidents start with **cache poisoning fears**, **origin meltdown**, or **geo routing mistakes**. Edge teams need to slash TTLs, shift origin weights, or bypass cache for one path — now. Traditional CDN APIs take minutes; YAML-at-edge takes longer.

[Kiponos.io](https://kiponos.io) feeds Java **control plane** services that compile policies pushed to edges. Rules live in a hub tree; controllers read locally when generating southbound config.

## Control plane compile step

```java
public EdgePolicy compilePolicy(String propertyId) {
    var cache = kiponos.path("cdn", propertyId, "cache");
    var origin = kiponos.path("cdn", propertyId, "origin");
    return EdgePolicy.builder()
        .defaultTtlSec(cache.getInt("default_ttl_sec"))
        .staleWhileRevalidate(cache.getInt("swr_sec"))
        .primaryOriginWeight(origin.getInt("primary_weight"))
        .failoverOrigin(origin.get("failover_host"))
        .bypassPaths(cache.get("bypass_paths_csv"))
        .build();
}
```

## CDN tree

```yaml
cdn/
  news-site/
    cache/
      default_ttl_sec: 300
      swr_sec: 60
      bypass_paths_csv: /api/live,/breaking/*
    origin/
      primary_weight: 90
      failover_host: origin-backup.example.com
    geo/
      eu_traffic_to: eu-pool
```

## Real-world scenarios

| Scenario | Live tweak |
|----------|------------|
| Origin overload | Lower TTL, raise failover weight |
| Live blog | Add `/breaking/*` to bypass |
| DDoS on asset class | Short TTL + stricter cache key rules |
| Migration | Shift `primary_weight` gradually |

## Performance

Compile runs periodically or on change listener — reads are **local**.

## Getting started

1. [kiponos.io](https://kiponos.io) — `cdn/{property}/*`
2. Map one property's rules into tree
3. Trigger compile; change TTL live; recompile without redeploy

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for Java. CDN policy at control-plane speed.*