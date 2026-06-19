---
title: "Multi-Tenant Kubernetes Namespaces on One Live Kiponos Hub (Java SDK)"
published: true
tags: java, kubernetes, multitenancy, realtime
description: Different K8s namespaces use different Kiponos profile paths — same hub, isolated trees, live updates without per-namespace ConfigMaps.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-multitenant.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-k8s-multitenant.jpg
---

Platform teams run **many namespaces** — each with its own ConfigMap copies and drift risk. Kiponos uses **profile paths** for isolation:

| Namespace | JVM property `kiponos` |
|-----------|------------------------|
| `team-a-prod` | `['app']['v2']['prod']['team-a']` |
| `team-b-prod` | `['app']['v2']['prod']['team-b']` |

Same container image everywhere. Only the profile string in the Deployment differs (via one env-as-bootstrap, or init metadata). All runtime config reads from SDK — **no per-namespace ConfigMap objects**.

Central ops can still see all teams in one hub; team leads edit only their subtree.

[kiponos.io](https://kiponos.io) · [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)