---
title: "GitOps vs Live Operational Config — Where Kiponos Fits in Platform Architecture"
published: false
tags: architecture, gitops, devops, config
description: GitOps owns desired state. Production still needs live thresholds, emergency overrides, and cross-service coordination. A architecture guide for platform teams drawing the boundary.
canonical_url: https://dev.to/kiponos/gitops-vs-live-operational-config-where-kiponos-fits-in-platform-architecture-2nbb
main_image: https://files.catbox.moe/y1msbh.jpg
---

Platform teams adopt **GitOps** so every cluster change is reviewed, versioned, and reconciled. That is the right default for **desired infrastructure state**.

Then production asks for something Git was never meant to optimize: *"Drop the fraud block score from 90 to 85 for the next twenty minutes while we investigate."* A PR, image rebuild, and Argo sync are the wrong latency for an operational knob.

[Kiponos.io](https://kiponos.io) is a **live config hub** — not a replacement for GitOps. It is the layer **below** GitOps that holds **operational parameters** services read at runtime with zero-latency local cache.

## Two different config classes

| Class | Examples | Right home |
|-------|----------|------------|
| **Declarative desired state** | Replica count, Ingress host, RBAC, image tag | Git → GitOps → cluster |
| **Operational runtime parameters** | Rate limits, circuit thresholds, model routing weights, feature percentages | Live hub + SDK read |

Confusing the two is why teams either (a) open emergency PRs at 3am, or (b) stuff operational floats into ConfigMaps and pretend that is GitOps.

## What GitOps does brilliantly

- Audit trail via commit history
- Peer review before reconcile
- Drift detection against declared YAML
- Repeatable promotions dev → staging → prod **for infrastructure**

Keep Helm charts, Kustomize overlays, and Argo CD applications in Git. Do not rip that out.

## Where GitOps hits the wall

```
Incident: payment decline rate spiked
Need: fraud.block_score 90 → 85 NOW
GitOps path: branch → PR → merge → pipeline → deploy → 25+ minutes
Ops path: dashboard tweak → WebSocket delta → SDK cache → sub-second
```

Other wall cases:

- **Saga step timeouts** tuned during load test week
- **LLM temperature / routing** adjusted while GPUs saturate
- **Canary percentage** shifted without a new Deployment manifest
- **Cross-service handoff flags** coordinating three teams mid-incident

These are **operational**, not **declarative**. They change hourly; they rarely belong in a Helm values PR.

## Reference architecture

![Architecture diagram](https://files.catbox.moe/z2kn7r.png)

```
Git (GitOps)          Kiponos Hub (live ops)
     │                        │
     ▼                        ▼
 Deployments            SDK in each service
 Ingress/RBAC           get() on hot path
 ConfigMaps (bootstrap)  WebSocket deltas
```

**Bootstrap in Git, operate in hub.** Ship a minimal Kiponos profile path in Git (team id, default tree skeleton). Day-2 tuning happens in the dashboard with ACL — not in a hundred YAML forks.

## Config tree example

```
['payments']['fraud']['prod']['live']
  block_score: 85
  review_score: 70
  max_velocity_per_min: 1200

['payments']['resilience']['prod']['live']
  circuit_failure_threshold: 0.45
  bulkhead_max_concurrent: 200
```

Java service on the hot path:

```java
Kiponos kiponos = Kiponos.createForCurrentTeam();
int blockScore = kiponos.path("fraud", "prod", "live").getInt("block_score");
```

No network on `get()` — SDK serves from in-process cache updated by delta stream.

## Governance without Git for every tweak

| Control | GitOps | Kiponos |
|---------|--------|---------|
| Who may change prod | CODEOWNERS | Dashboard ACL per profile |
| Audit | `git log` | Hub change log + actor |
| Emergency break-glass | Hotfix branch | Live override + post-incident sync to Git |
| Rollback | `git revert` | One-click tree restore |

**Recommended policy:** operational keys live in Kiponos; **structural** keys (new service URLs after migration) still go through Git review, then seed the hub.

## Anti-patterns

1. **Every float in a ConfigMap** — triggers pod reload culture; merges GitOps with ops tuning badly.
2. **Feature-flag SaaS for circuit breakers** — wrong tool; boolean-centric, network-bound evaluation.
3. **SSH + `kubectl edit`** — no audit, no SDK contract, breaks compliance story.
4. **Duplicate YAML per env** — see [multi-env chaos article](https://dev.to/kiponos/escape-multi-environment-configuration-chaos-with-one-kiponos-profile-per-env-java-sdk-3205).

## When to choose what

| Need | Use |
|------|-----|
| New microservice Deployment | GitOps |
| Rotate TLS via cert-manager | GitOps |
| Tune API rate limit during traffic spike | Kiponos |
| Shift 5% canary traffic | Kiponos (or mesh + Kiponos weights) |
| Add Redis StatefulSet | GitOps |

## Getting started alongside Argo / Flux

1. Keep GitOps for cluster desired state — unchanged.
2. [Create TeamPro at kiponos.io](https://kiponos.io) — one profile path per environment.
3. Migrate **operational** keys out of `values-prod.yaml` into the hub; leave bootstrap secrets in Git/sealed-secrets.
4. Document boundary in your platform RFC: *"Git declares wiring; hub declares knobs."*
5. Run game days: measure time-to-mitigate with hub tweak vs PR path.

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — GitOps for what you deploy. Live config for how it behaves in production.*