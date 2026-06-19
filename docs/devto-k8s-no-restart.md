---
title: "Config Changes in Kubernetes Without Pod Restart or Redeploy (Kiponos SDK)"
published: true
tags: java, kubernetes, devops, realtime
description: Change any runtime parameter in Kiponos dashboard — pods keep running, SDK applies deltas locally. End the ConfigMap rollout cycle.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-no-restart.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-k8s-no-restart.jpg
---

The default K8s config story: edit ConfigMap → rolling restart → hope nothing breaks during surge. For **operational tuning**, that is too slow and too risky.

With Kiponos in the pod:

1. JVM connects at startup
2. Ops changes `feature_x_enabled` in dashboard
3. WebSocket pushes delta to **every running pod**
4. Next `kiponos.get("feature_x_enabled")` returns new value

**No `kubectl rollout restart`.** No new image. No Helm upgrade.

Ideal for feature flags, rate limits, integration endpoints, and incident response — everything that used to live in ConfigMaps.

[kiponos.io](https://kiponos.io) · [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)