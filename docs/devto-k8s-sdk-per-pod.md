---
title: "One Kiponos SDK per Pod — Local Reads at Cluster Scale (Java)"
published: true
tags: java, kubernetes, architecture, realtime
description: Every pod runs its own SDK instance with in-memory config cache. Horizontal scale without shared config servers or Redis polls.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-sdk-per-pod.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-k8s-sdk-per-pod.jpg
---

Scaling out usually means **shared config infrastructure** — Redis, etcd, or a central config API that becomes a bottleneck. Kiponos inverts this: **each pod is autonomous**.

- One WebSocket per JVM
- Full config tree cached locally
- Reads never leave the process
- Hub broadcasts deltas to all connected pods

HPA adds pods → new pods connect and hydrate → they join the live config mesh. No ConfigMap version skew across replicas because there is no mounted version — only **latest in memory**.

```java
// Same read path on pod 1 and pod 500
int limit = kiponos.path("api", "limits").getInt("rpm");
```

Perfect for stateless Java microservices on Kubernetes.

[kiponos.io](https://kiponos.io) · [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)