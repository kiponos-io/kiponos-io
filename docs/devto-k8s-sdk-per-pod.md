---
title: "One Kiponos SDK Per Pod — Local Reads at Kubernetes Scale (Java SDK)"
published: true
tags: java, kubernetes, architecture, realtime
description: Every pod runs its own SDK with an in-memory config cache. Reads are local at any replica count — WebSocket deltas keep the fleet consistent without a central config bottleneck.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-sdk-per-pod.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-k8s-sdk-per-pod.jpg
---

At 200 replicas, "just poll the config server" becomes a **thundering herd**. Central Redis for config sounds fine until every pod hits it on cache miss during an incident. Kubernetes wants **horizontal scale**; your config layer must not become a **serial bottleneck**.

[Kiponos.io](https://kiponos.io) uses **one SDK instance per pod**, each holding a full in-memory copy of the profile tree. Reads are **always local**. Updates fan out as **WebSocket deltas** from the hub — no pod polls a central store on the request path.

## Scale-out architecture

![Architecture diagram](https://files.catbox.moe/w46ke5.png)

HPA adds Pod N+1 → new SDK connects → receives snapshot + subscribes to deltas → **same read semantics** as Pod 1.

## Request path at scale

```java
// Called thousands of times per second per pod
public boolean allowRequest(String tenantId) {
    return localCounter(tenantId).tryAcquire(
        kiponos.path("limits", tenantId).getInt("rpm")
    );
}
```

No shared Redis read on `allowRequest`. Each pod's SDK cache serves the hot path.

## Consistency model

| Question | Answer |
|----------|--------|
| Are all pods identical? | Converge via WebSocket — typically sub-second |
| Strong consistency required? | Use versioning in business logic if needed |
| What on connect? | Full tree snapshot, then deltas |
| Pod crash? | Replacement gets latest on startup |

For rate limits and feature flags, **eventual consistency across pods** is standard — same as ConfigMap propagation without restart lag.

## Memory footprint

Each pod holds one tree for its profile. Typical service config (hundreds of keys) is **kilobytes to low megabytes** — negligible vs JVM heap for business objects.

Compare to:

- Duplicating entire ConfigMap per pod (same order)
- Per-pod polling config server (CPU + network worse)

## Real-world scenarios

| Scenario | Per-pod SDK behavior |
|----------|------------------------|
| HPA scale-up | New pods auto-sync |
| Global flag flip | Delta fan-out to all connections |
| Tenant-specific limit | Local read per request |
| Region-specific profile | Different `-Dkiponos` per Deployment |

## Performance

- **O(1) reads** per key per pod
- **No central QPS** scaling with request rate
- **One WebSocket** per pod — hub handles fan-out, not per-request load

## Compare to alternatives

| Approach | Hot-path read | Scales with replicas |
|----------|---------------|----------------------|
| Central config HTTP | RTT per read | Poor |
| Redis cache per read | RTT + invalidation | Medium |
| ConfigMap file watch | Local after mount | Restart to update |
| **Kiponos SDK per pod** | **Local** | **Linear (independent caches)** |

## Getting started

1. Ensure every pod has `KIPONOS_ID`, `KIPONOS_ACCESS`, profile JVM arg
2. Load test at HPA max — confirm no config-related latency spike
3. Flip a global flag; watch all pods react via logs/`afterValueChanged`

Pair with: [no restart updates](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-no-restart.md)

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for Java at scale. Every pod reads locally; the hub keeps them in sync.*