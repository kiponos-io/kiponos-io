---
title: "Kubernetes Pods Without ConfigMaps or Environment Variables (Kiponos Java SDK)"
published: true
tags: java, kubernetes, devops, realtime
description: Remove ConfigMaps, Secrets mounts, and env-var walls from pods. Each container uses Kiponos SDK for live local config reads.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-no-configmaps.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-k8s-no-configmaps.jpg
---

Kubernetes config is a stack of pain: ConfigMaps, Secrets, envFrom, volume mounts, reload operators, and restart annotations. Every change touches manifests or triggers rolling updates.

**Kiponos removes pod configuration entirely.**

Pod spec needs only:

```yaml
env:
  - name: KIPONOS_ID
    valueFrom: secretKeyRef: ...
  - name: KIPONOS_ACCESS
    valueFrom: secretKeyRef: ...
# No ConfigMap. No DATABASE_URL env. No application.yml mount.
```

Application code:

```java
Kiponos kiponos = Kiponos.createForCurrentTeam();
String db = kiponos.path("data", "postgres").get("host");
```

All operational config lives in Kiponos hub; SDK holds it **in memory** per pod. Updates arrive via WebSocket delta — **no ConfigMap reload, no pod restart**.

Auth tokens are the only K8s secrets you mount — not your business configuration surface.

[kiponos.io](https://kiponos.io) · [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)