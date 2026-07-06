---
title: "HPA Scale-Down Stabilization You Can Tune Mid-Incident — Without kubectl edit (Kiponos Java SDK)"
published: false
tags: java, kubernetes, devops, finops
description: stabilizationWindowSeconds is buried in HPA behavior YAML. After a traffic spike ends, five-minute scale-down windows burn budget; during noisy metrics you need longer windows — Kiponos feeds a Java HPA behavior reconciler.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-scale-down-stabilization.md
main_image: https://files.catbox.moe/vkgtrb.jpg
---

Sunday 23:10 UTC. The flash sale ends. Checkout RPS drops from **18,000** to **3,200** in twelve minutes. Your HPA scaled to **38 pods** at peak. Now CPU is **22%** on every replica — but pod count will not move for another **four minutes** because `behavior.scaleDown.stabilizationWindowSeconds: 300` is baked into the manifest.

FinOps pings the incident channel:

> "We are paying for **35 idle pods**. Why is scale-down stuck?"

Because Kubernetes is doing exactly what you told it in Git: wait five minutes, take the **maximum** recommendation over the window, then drop at most **10%** per minute. That was correct when metrics were noisy. Tonight traffic **collapsed cleanly** — the stabilization window is now **cost**, not stability.

Someone runs `kubectl edit hpa checkout-api`. Argo CD flags **OutOfSync**. Tomorrow's standup debates whether emergency edits belong in Git or in muscle memory.

**The Aha:** `stabilizationWindowSeconds` is **operational**, not architectural. A Java **hpa-behavior-controller** reads `scaling/scale_down/*` from [Kiponos.io](https://kiponos.io) and patches HPA `spec.behavior` when ops changes the window — **no checkout redeploy, no Helm merge during the FinOps ping**.

## The problem — frozen scale-down behavior

```yaml
# charts/checkout/templates/hpa.yaml
spec:
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Percent
          value: 10
          periodSeconds: 60
      selectPolicy: Max
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
        - type: Percent
          value: 100
          periodSeconds: 15
```

Scale-up is aggressive (good for incidents). Scale-down is conservative (good for noisy CPU). **Same policy for every night** — including nights when traffic falls off a cliff.

| What teams believe | What production reality |
|--------------------|-------------------------|
| "300s prevents flapping" | Also prevents FinOps from sleeping |
| "We'll lower it in next week's Helm release" | Idle pod cost is **tonight** |
| "Cluster autoscaler removes nodes" | HPA still holds 38 pod objects |
| "VPA handles rightsizing" | VPA does not replace HPA scale-down delay |

## What Kiponos.io is — for scale-down policy

[Kiponos.io](https://kiponos.io) stores **scale-down knobs** beside your HPA target policy ([live HPA targets](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-hpa-target-live.md)). A small Java controller Deployment watches the hub tree and the Kubernetes HPA API.

Profile:

```
['checkout']['prod']['scaling']
```

On `afterValueChanged` for `scaling/scale_down/*`, the controller patches `HorizontalPodAutoscaler.spec.behavior.scaleDown` via the K8s client. Values are **local reads** in the controller JVM — no polling Redis, no ConfigMap volume reload.

Checkout pods keep serving traffic with their embedded SDK; only the controller touches HPA objects.

## Architecture

![Architecture diagram](https://files.catbox.moe/r4oau3.png)

## Config tree

```yaml
scaling/
  scale_down/
    stabilization_window_seconds: 300
    percent_per_period: 10
    period_seconds: 60
    select_policy: Max
    fast_scale_down_mode: false
    fast_stabilization_window_seconds: 60
    fast_percent_per_period: 25
  scale_up/
    stabilization_window_seconds: 0
    percent_per_period: 100
    period_seconds: 15
  guards/
    min_replicas_floor: 6
    block_scale_down_during_incident: false
    incident_flag_key: checkout_sev1
  finops/
    idle_cpu_threshold_percent: 25
    enable_cost_recovery_mode: false
    cost_recovery_window_seconds: 120
```

## Integration — HpaBehaviorReconciler

```java
@Configuration
public class K8sClientConfig {

    @Bean
    public Kiponos kiponos(@Value("${kiponos.team-id}") String teamId,
                           @Value("${kiponos.access-key}") String accessKey) {
        return Kiponos.builder()
                .teamId(teamId)
                .accessKey(accessKey)
                .profilePath("['checkout']['prod']['scaling']")
                .build();
    }

    @Bean
    public KubernetesClient kubernetesClient() {
        return new KubernetesClientBuilder().build();
    }
}
```

```java
@Component
public class HpaBehaviorReconciler {

    private final Kiponos kiponos;
    private final KubernetesClient k8s;
    private final String namespace;
    private final String hpaName;

    public HpaBehaviorReconciler(Kiponos kiponos, KubernetesClient k8s,
                                 @Value("${controller.namespace}") String namespace,
                                 @Value("${controller.hpa-name}") String hpaName) {
        this.kiponos = kiponos;
        this.k8s = k8s;
        this.namespace = namespace;
        this.hpaName = hpaName;
        kiponos.afterValueChanged(c -> {
            if (c.path().startsWith("scaling/scale_down")
                    || c.path().startsWith("scaling/finops")) {
                reconcile();
            }
        });
        reconcile();
    }

    public void reconcile() {
        var down = kiponos.path("scaling", "scale_down");
        var finops = kiponos.path("scaling", "finops");
        var guards = kiponos.path("scaling", "guards");

        if (guards.getBool("block_scale_down_during_incident", false)) {
            return;
        }

        int window;
        int percent;
        if (finops.getBool("enable_cost_recovery_mode", false)) {
            window = finops.getInt("cost_recovery_window_seconds", 120);
            percent = down.getInt("fast_percent_per_period", 25);
        } else if (down.getBool("fast_scale_down_mode", false)) {
            window = down.getInt("fast_stabilization_window_seconds", 60);
            percent = down.getInt("fast_percent_per_period", 25);
        } else {
            window = down.getInt("stabilization_window_seconds", 300);
            percent = down.getInt("percent_per_period", 10);
        }
        int period = down.getInt("period_seconds", 60);
        String select = down.get("select_policy", "Max");

        var hpa = k8s.autoscaling().v2().horizontalPodAutoscalers()
                .inNamespace(namespace)
                .withName(hpaName)
                .get();
        if (hpa == null) {
            return;
        }

        var behavior = Optional.ofNullable(hpa.getSpec().getBehavior())
                .orElseGet(HorizontalPodAutoscalerBehavior::new);
        var scaleDown = Optional.ofNullable(behavior.getScaleDown())
                .orElseGet(HPAScalingRules::new);
        scaleDown.setStabilizationWindowSeconds(window);
        scaleDown.setSelectPolicy(select);
        scaleDown.setPolicies(List.of(new HPAScalingPolicyBuilder()
                .withType("Percent")
                .withValue(percent)
                .withPeriodSeconds(period)
                .build()));
        behavior.setScaleDown(scaleDown);
        hpa.getSpec().setBehavior(behavior);

        k8s.autoscaling().v2().horizontalPodAutoscalers()
                .inNamespace(namespace)
                .resource(hpa)
                .patch();
    }
}
```

Sunday night FinOps enables `enable_cost_recovery_mode: true` → window drops to **120s**, percent rises to **25** → idle pods drain faster. Monday morning traffic is steady but CPU metrics are noisy → ops disables cost recovery and restores `stabilization_window_seconds: 300` — **no Helm PR**.

## Real scenarios

| Event | Frozen 300s window | Kiponos scale-down policy |
|-------|-------------------|---------------------------|
| Flash sale ends | 35 idle pods × 5 min | `enable_cost_recovery_mode: true` |
| Noisy CPU after deploy | Scale-down flapping | Raise window to `600` live |
| Sev1 incident | HPA drops pods mid-fix | `block_scale_down_during_incident: true` |
| Weekend quiet | Over-provisioned all Sunday | `fast_scale_down_mode: true` |
| Multi-env | staging vs prod YAML drift | Separate profile paths per env |

## Performance

- Controller: single WebSocket; reconcile runs on **delta**, not per checkout request
- `getInt("stabilization_window_seconds")` is local — reconcile completes in milliseconds
- Checkout pods: **zero** extra work on scale-down policy change
- HPA patch is **one K8s API call** per policy change — not per pod
- New pods from later scale-up still use Kiponos SDK snapshot ([HPA interaction](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-no-restart.md))

## Compare to alternatives

| Approach | Sunday night cost recovery | Audit trail |
|----------|---------------------------|-------------|
| `kubectl edit hpa` | Fast | Breaks GitOps; weak audit |
| Helm values PR | Slow | Git log |
| KEDA cooldown only | Partial | Does not map to HPA stabilization |
| Fixed `stabilizationWindowSeconds` in Git | Stable | Wrong for clean traffic cliffs |
| **Kiponos + reconciler** | **Dashboard toggle** | **Hub change log** |

## When not to use Kiponos for scale-down

| Case | Better approach |
|------|-----------------|
| `maxReplicas` / node pool ceiling | GitOps + FinOps capacity planning |
| Replacing HPA with manual replica count | Architecture decision |
| Compliance: all HPA YAML must be Git-sourced | Post-incident sync hub → Git |
| One-shot cluster scale-down for maintenance | `kubectl scale` with change ticket |

## Getting started (15 minutes)

1. [TeamPro at kiponos.io](https://kiponos.io) — extend profile `['checkout']['prod']['scaling']` with `scale_down/`.
2. Deploy **hpa-behavior-controller** with RBAC `patch horizontalpodautoscalers`.
3. Seed `stabilization_window_seconds: 300` matching current Git default.
4. Game day: scale staging up, drop load, flip `enable_cost_recovery_mode`, watch replica count fall faster.
5. Document: Git owns **ceiling**; hub owns **scale-down patience**.

**Further reading:**

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [Live HPA targets](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-hpa-target-live.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — scale-down waits as long as tonight needs, not forever.*