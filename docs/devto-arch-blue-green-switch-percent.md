---
title: "Blue-Green Switch Percent Without Load Balancer Terraform — Live Traffic Cutover in Java (Java SDK)"
published: false
tags: java, architecture, deployment, devops
description: Blue-green cutover trapped in ALB rules means rollback waits on Terraform. Kiponos holds green weight, drain flags, and sticky session policy in one live tree — SRE shifts traffic while JVMs keep running.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-blue-green-switch-percent.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-arch-blue-green-switch-percent.jpg
---

Friday 11:34 UTC. **payments-green** passed smoke tests. The release manager opens the blue-green runbook: step 5 says *"update `green_weight` in `alb-rules.tf` from 0 to 10, apply Terraform."* Step 8 says *"watch error rate for 30 minutes, then 50, then 100."* Step 12 — rollback — says *"revert Terraform commit."*

Twenty-two minutes into the 10% cutover, green returns `HTTP 502` on 8% of card tokenizations. The on-call SRE needs **0% green NOW**. The Terraform plan is queued behind three other infra PRs. Blue still serves 90% but the router at the edge has no idea green is on fire — weights live in state file, not in the app.

The VP of engineering asks:

> "Why does **rollback** require infra pipeline latency when the failure is **application errors**?"

Most Java payment edges encode blue-green posture as **three different artifacts**: Terraform ALB weights, a wiki cutover checklist, and hard-coded `"blue"` backend selection in a router nobody has changed since the last major release. [Kiponos.io](https://kiponos.io) collapses that into **one operational tree** — green weight percent, drain semantics, and backend URLs — readable on every routing decision with **local `get*()` calls** and adjustable from the dashboard while processes run.

## The problem: cutover percent baked into load balancer config

A typical edge router picks color like this:

```java
@Service
public class PaymentColorRouter {
    private static final int GREEN_WEIGHT = 0;  // Terraform said 0 at last apply

    public String pickColor(String merchantId) {
        int roll = Math.floorMod(merchantId.hashCode(), 100);
        return roll < GREEN_WEIGHT ? "green" : "blue";
    }
}
```

Cutover policy usually lives elsewhere — scattered and static:

```hcl
# alb-rules.tf — minutes to apply, seconds matter during rollback
resource "aws_lb_listener_rule" "green_weight" {
  action {
    forward {
      target_group {
        arn    = aws_lb_target_group.green.arn
        weight = 10
      }
      target_group {
        arn    = aws_lb_target_group.blue.arn
        weight = 90
      }
    }
  }
}
```

Or worse — only at the ALB, while the Java edge still routes 100% blue because it never read the Terraform output:

```yaml
# application-prod.yml — stale after emergency manual ALB tweak
routing:
  green_weight_pct: 0
  sticky_sessions: true
```

During cutover you need to:

1. Raise **`cutover.green_weight_pct`** in controlled steps — 5 → 10 → 25
2. Flip **`cutover.emergency_rollback`** to force 0% green instantly
3. Tune **`cutover.sticky_by_merchant`** so session affinity survives partial shifts

Doing that through Terraform while tokenization errors spike is not continuous delivery — it is **infrastructure theater on the critical path**.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "ALB weights are the source of truth" | App-layer routers and ALB disagree after manual tweaks |
| "10% canary is safe for payments" | High-value merchants cluster in hash buckets |
| "Terraform rollback is fast" | State lock, plan review, and apply = minutes |
| "Blue-green is binary — flip DNS" | Partial cutover needs percent knobs, not boolean DNS |
| "Sticky sessions belong in the load balancer only" | App router must honor same stickiness contract |

## The architecture insight

**Blue-green cutover percent is operational config, not Terraform archaeology.** The same knobs your release runbook tells humans to edit — green weight, emergency rollback, sticky policy — belong in **one live tree** the JVM already reads on every routing decision. Kiponos makes "0% green NOW" a **dashboard edit**, not an infra pipeline.

## What Kiponos.io is for blue-green cutover

[Kiponos.io](https://kiponos.io) is a real-time configuration hub. Each Spring Boot edge service connects **once** at startup over WebSocket; the profile tree — for example `['payments']['edge']['prod']['live']` — loads into an **in-memory cache** inside the Java SDK.

When SRE sets `cutover.green_weight_pct` to `0` during rollback, a **delta** patches only that key. The next `kiponos.path("cutover").getInt("green_weight_pct")` on an incoming tokenization request is a **local memory read** — no HTTP to a config API, no JDBC poll, no Redis round-trip on the payment path.

`afterValueChanged` listeners let you log audit trails, increment `cutover_rollback_total`, and warm connection pools toward blue backends **without** restarting the JVM.

No restart. No redeploy. No `@RefreshScope` bean recycle.

## Reference architecture

![Architecture diagram](https://litter.catbox.moe/6ceglb.png)

**Terraform provisions colors; the tree drives live percent.** Keep ALB target groups in IaC — but the **authoritative cutover weight** lives in Kiponos where rollback takes seconds.

## Config tree — cutover, backends, and drain in one place

Five folders — `cutover`, `backends`, `drain`, `health`, `audit`:

```yaml
cutover/
  green_weight_pct: 0
  max_green_ceiling_pct: 50
  emergency_rollback: false
  sticky_by_merchant: true
  step_schedule_enabled: false
backends/
  blue_url: https://payments-blue.internal
  green_url: https://payments-green.internal
  active_health_check_sec: 15
drain/
  reject_new_on_green: false
  grace_inflight_sec: 120
health/
  green_error_rate_pct: 0.0
  auto_rollback_above_pct: 5.0
  last_health_sync_ms: 0
audit/
  last_weight_change_by: ""
  last_weight_change_at_ms: 0
  last_rollback_by: ""
```

One tree. One profile path: `['payments']['edge']['prod']['live']`. Staging cutover rehearsals share **identical key layout** — only values differ.

## Java integration: color router + live weight reads

```java
import io.kiponos.sdk.Kiponos;
import org.springframework.stereotype.Service;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class BlueGreenPaymentRouter {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();
    private final AtomicReference<String> greenBaseUrl = new AtomicReference<>();

    public BlueGreenPaymentRouter() {
        kiponos.afterValueChanged(change -> {
            if (change.path().startsWith("backends/") || change.path().startsWith("cutover/")) {
                log.info("Cutover delta: path={} value={}", change.path(), change.newValue());
            }
        });
        greenBaseUrl.set(kiponos.path("backends").get("green_url"));
    }

    public RouteTarget route(String merchantId, TokenizeRequest req) {
        var cutover = kiponos.path("cutover");
        var drain = kiponos.path("drain");

        if (cutover.getBool("emergency_rollback")) {
            return toBlue("emergency_rollback");
        }

        int greenPct = effectiveGreenWeight(cutover);
        String color = pickColor(merchantId, greenPct, cutover.getBool("sticky_by_merchant"));

        if ("green".equals(color) && drain.getBool("reject_new_on_green")) {
            return toBlue("green_draining");
        }

        return "green".equals(color)
            ? RouteTarget.green(greenBaseUrl.get())
            : toBlue("weighted_route");
    }

    private int effectiveGreenWeight(Kiponos.PathView cutover) {
        int pct = cutover.getInt("green_weight_pct");
        return Math.min(pct, cutover.getInt("max_green_ceiling_pct"));
    }

    private String pickColor(String merchantId, int greenPct, boolean sticky) {
        int bucket = sticky
            ? Math.floorMod(merchantId.hashCode(), 100)
            : ThreadLocalRandom.current().nextInt(100);
        return bucket < greenPct ? "green" : "blue";
    }

    private RouteTarget toBlue(String reason) {
        return RouteTarget.blue(
            kiponos.path("backends").get("blue_url"), reason);
    }
}

@Service
public class CutoverHealthGuard {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();

    public void onGreenErrorRate(double errorPct) {
        kiponos.path("health").set("green_error_rate_pct", errorPct);
        var health = kiponos.path("health");
        if (errorPct > health.getFloat("auto_rollback_above_pct")) {
            kiponos.path("cutover").set("emergency_rollback", true);
            kiponos.path("cutover").set("green_weight_pct", 0);
            kiponos.path("audit").set("last_rollback_by", "health_guard_auto");
        }
    }
}
```

Every `getInt()`, `getBool()`, and `get()` on the routing path is **O(1) local cache** — microseconds, not cross-region config service RTT.

Wire `CutoverHealthGuard` to your metrics pipeline so green error spikes trigger **automatic 0% rollback** before the Terraform PR merges.

## Real-world scenarios

| Scenario | Without live cutover tree | With Kiponos one-tree blue-green |
|----------|---------------------------|----------------------------------|
| Green 502 spike at 10% | Revert Terraform; minutes | `emergency_rollback: true` + `green_weight_pct: 0` |
| Scheduled 5→10→25 cutover | Three Terraform applies | Dashboard edits; same JVMs |
| Drain green after shift | Manual target group deregistration | `drain/reject_new_on_green: true` |
| Staging rehearsal | Different env var than prod | Same tree shape; rehearsal flips real keys |
| Recovery — retry cutover | Second infra wave | Reset rollback flag; step weight live |

## Performance: why cutover gates must not add network I/O

- **One WebSocket per JVM** — not one config fetch per tokenization request
- **Color decision is three local reads** — nanoseconds vs card-network I/O
- **Delta patches** — zeroing green weight sends one patch, not full tree reload
- **Backend URL via listener** — pool rebuild once per key change, not per transaction
- **No GC pressure** from re-parsing routing YAML on Black Friday traffic spikes

In load tests, Kiponos reads are noise on the tokenization path; issuer network RTT dominates latency.

## Compare to alternatives

| Approach | Emergency rollback speed | Hot-path read latency | Single tree for weight + drain + backends |
|----------|-------------------------|----------------------|-------------------------------------------|
| Terraform ALB weights | Minutes — state lock + apply | Zero at ALB only | No — app router may disagree |
| Kubernetes Ingress weights | Minutes — reconcile | Zero if edge-only | Partial — no app stickiness |
| Redis routing hash | Yes with poll | Poll interval adds tail latency | Possible — custom schema discipline |
| Feature-flag SaaS | Booleans only | SDK network on evaluation | No — percent weights awkward |
| DNS blue-green flip | Slow TTL propagation | Zero (static) | No — binary, not gradual |
| **Kiponos SDK** | **Yes — dashboard delta** | **Zero (in-process cache)** | **Yes — one profile tree** |

## When not to use Kiponos for blue-green cutover

| Boundary | Better home |
|----------|-------------|
| ALB target group provisioning, TLS certs, listener ARNs | Terraform / cloud IaC |
| Database schema migration and backward-compatible DDL | Git-reviewed migration scripts |
| Immutable container image digests and deployment manifests | GitOps → cluster reconcile |
| Secrets for green environment API keys | Vault / sealed-secrets — not live dashboard |
| Physical network isolation between blue and green VLANs | Network engineering runbook |

## Getting started (15 minutes)

1. [Create TeamPro at kiponos.io](https://kiponos.io) — add profile `['payments']['edge']['prod']['live']` with `cutover`, `backends`, and `drain` folders matching the tree above.
2. Add `io.kiponos:sdk-boot-3` to your Spring Boot payment edge router.
3. Set `KIPONOS_ID`, `KIPONOS_ACCESS`, and `-Dkiponos="['payments']['edge']['prod']['live']"`.
4. Replace static `GREEN_WEIGHT` with `kiponos.path("cutover").getInt("green_weight_pct")`.
5. Register `BlueGreenPaymentRouter` and wire `CutoverHealthGuard` to error-rate metrics.
6. Rehearsal: in staging, step `green_weight_pct` 0 → 10 → 0 — confirm routing shifts **without pod restart**. Document key names in your cutover runbook.

## Further reading

- [Developer Quickstart](https://dev.to/kiponos/kiponosio-developer-quickstart-java-python-and-your-first-live-config-change-3kjo)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
- Related: [Canary traffic weights](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-canary-traffic-weights.md)
- Related: [Disaster recovery live config](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-disaster-recovery-live-config.md)

---

*Kiponos.io — Terraform builds the colors; the tree decides live percent.*