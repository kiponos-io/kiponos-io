---
title: "Traffic Steering Weights Live — Shift Edge Paths Without DNS Terraform (Java SDK)"
published: false
tags: java, cdn, edge, architecture
description: Traffic weights in load balancer TF are slow. Kiponos holds steering percentages for edge routers.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-edgeops-traffic-steering-weights-live.md
main_image: https://files.catbox.moe/uiggaj.jpg
---

Friday 09:41 UTC. **origin-primary** error rate hits **18%** — circuit breakers flapping — while **origin-backup** in the secondary POP sits at **2% utilization**. Traffic steering still sends **90%** to primary because `primary_weight_pct: 90` lives in a Terraform weighted target group last applied Tuesday. Shifting **30% to backup** without a DNS TTL cliff or load balancer apply window means opening an infra PR while checkout 5xx climbs.

The edge platform lead:

> "Steering weight is an **incident knob** — not desired state. I need to slide **`primary_weight_pct`** from 90 → 60 **now**, watch error budgets, slide back when primary heals."

Java edge routers and custom LB sidecars read weights from boot config:

```java
private static final int PRIMARY_WEIGHT = 90;
private static final int BACKUP_WEIGHT = 10;
```

[Kiponos.io](https://kiponos.io) holds **live steering percentages** — primary, backup, canary POP weights — in a tree edge routers read on every upstream selection with **local `getInt()`**. Shift traffic during origin degradation in seconds, not Terraform apply minutes.

## The problem — `primary_weight_pct` trapped in load balancer state

Typical infrastructure encoding:

```hcl
# alb-steering.tf — apply-bound
resource "aws_lb_target_group_attachment" "primary" {
  weight = 90
}
resource "aws_lb_target_group_attachment" "backup" {
  weight = 10
}
```

Application-level weighted routing duplicates the problem:

```java
@Service
public class OriginSteeringRouter {
    private static final int PRIMARY_WEIGHT = 90;

    public OriginEndpoint select(List<OriginEndpoint> pool, ThreadLocalRandom rng) {
        int roll = rng.nextInt(100);
        return roll < PRIMARY_WEIGHT ? pool.get(0) : pool.get(1);
    }
}
```

During **origin degradation — shift 30% to backup POP live**, you need to:

1. Set **`steering.primary_weight_pct`** from 90 → 60
2. Raise **`steering.backup_weight_pct`** to 40
3. Enable **`guards.min_backup_floor_pct`** so backup never drops below 20 during incidents

ALB Terraform applies and DNS weighted records are **minutes**. Checkout errors are **seconds**.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Load balancer weights are ops tools" | Many orgs treat TF as **change-controlled desired state** |
| "DNS weighted routing is fast enough" | TTL cliffs **cache** bad steering during failures |
| "Failover is automatic" | Health checks lag; **gradual shift** beats hard failover |
| "Mesh traffic split replaces weights" | Mesh splits still often live in **Git**, not bridge |
| "90/10 is good enough" | Degraded primary at 90% **amplifies** error rate |

## The Aha

**Traffic steering weights are operational levers** — they slide when origin health degrades and restore when shards recover. Percentage knobs belong in a **live steering tree** the router reads on every `select()` call, not in Terraform that mocks your bridge while checkout burns.

## What Kiponos.io is for live steering

[Kiponos.io](https://kiponos.io) is a real-time configuration hub. Edge router JVMs connect via WebSocket; profile `['edge']['prod']['steering']` hydrates in-memory.

Ops sets `steering/primary_weight_pct` to 60 in the dashboard; delta reaches every router. Next weighted `select()` uses **60/40** locally — no ALB apply, no DNS change, no pod restart.

Honest boundary: Kiponos does **not** replace **which** origins exist (Terraform) or **global anycast DNS** topology. It owns **percentage weights** your **Java edge router** already implements in application code.

## Architecture

![Architecture diagram](https://files.catbox.moe/hi2hhs.png)

## Config tree — steering, pools, guards, audit

```yaml
steering/
  primary_weight_pct: 90
  backup_weight_pct: 10
  canary_weight_pct: 0
  sticky_session_enabled: true
pools/
  primary/
    endpoints: ["origin-primary.eu", "origin-primary.us"]
    health_ok: true
  backup/
    endpoints: ["origin-backup.eu"]
    health_ok: true
  canary/
    endpoints: ["origin-canary.eu"]
    health_ok: false
guards/
  min_backup_floor_pct: 10
  max_shift_per_edit_pct: 40
  auto_shift_on_primary_error_pct: 15
degradation/
  force_backup_mode: false
  shed_canary: true
audit/
  last_weight_change_by: ""
  primary_weight_before_incident: 90
```

Profile path: `['edge']['prod']['steering']`.

## Java integration — live weighted upstream selection

```java
import io.kiponos.sdk.Kiponos;
import org.springframework.stereotype.Service;

@Service
public class LiveSteeringRouter {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();
    private final OriginHealthMonitor healthMonitor;

    public LiveSteeringRouter(OriginHealthMonitor healthMonitor) {
        this.healthMonitor = healthMonitor;
        kiponos.afterValueChanged(change ->
            log.info("Steering weight delta: {} → {}", change.path(), change.newValue())
        );
    }

    public OriginEndpoint select( HttpRequest req) {
        var steering = kiponos.path("steering");
        int primary = effectivePrimaryWeight();
        int backup = effectiveBackupWeight();
        int canary = steering.getInt("canary_weight_pct", 0);

        int roll = ThreadLocalRandom.current().nextInt(primary + backup + canary);
        if (roll < primary) {
            return endpointFromPool("primary");
        }
        if (roll < primary + backup) {
            return endpointFromPool("backup");
        }
        return endpointFromPool("canary");
    }

    private int effectivePrimaryWeight() {
        if (kiponos.path("degradation").getBool("force_backup_mode")) {
            return 0;
        }
        int primary = kiponos.path("steering").getInt("primary_weight_pct", 90);
        int floor = kiponos.path("guards").getInt("min_backup_floor_pct", 10);
        return Math.min(primary, 100 - floor);
    }

    private int effectiveBackupWeight() {
        if (kiponos.path("degradation").getBool("force_backup_mode")) {
            return 100;
        }
        return kiponos.path("steering").getInt("backup_weight_pct", 10);
    }

    private OriginEndpoint endpointFromPool(String poolName) {
        var pool = kiponos.path("pools", poolName);
        if (!pool.getBool("health_ok") && !"backup".equals(poolName)) {
            return endpointFromPool("backup");
        }
        List<String> eps = pool.get("endpoints").asStringList();
        return OriginEndpoint.parse(eps.get(
            ThreadLocalRandom.current().nextInt(eps.size())));
    }
}
```

Weighted selection reads **three integers from local memory** — mandatory at 8k+ routing decisions per second per POP.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| Origin degradation — shift 30% to backup POP live | TF apply + health check lag | `primary_weight_pct: 60` — seconds |
| Primary heals | Manual TF revert PR | Restore `90/10` in one dashboard edit |
| Canary soak | DNS or mesh Git change | `canary_weight_pct: 5` live |
| Backup capacity guard | Overwhelmed backup | `guards.min_backup_floor_pct` enforced in code |
| Game day | Staging weights diverge | Same tree layout; rehearsal values only |

## Performance on the steering hot path

- **Weight lookup is 3–5 local reads** — nanoseconds vs origin RTT
- **Gradual shift beats hard failover** — error rate drops before backup saturates
- **Delta updates weights without connection pool drain** — no JVM recycle
- **Per-POP routers share profile** — consistent steering semantics globally
- **Guards cap max shift** — prevents operator typo sending 100% to cold backup

## Compare to alternatives

| Approach | Mid-incident weight slide | Hot-path read | App-level weighted select |
|----------|---------------------------|---------------|---------------------------|
| ALB Terraform weights | Apply minutes | N/A in app | Infra-only |
| DNS weighted records | TTL cache delay | N/A | Coarse |
| Envoy xDS | Strong; ops complexity | Control plane | Good |
| Redis hash poll | Yes with staleness | Poll interval | DIY |
| **Kiponos steering/** | **Seconds** | **Local getInt()** | **Native** |

## When not to use Kiponos

| Case | Use instead |
|------|-------------|
| Target group ARN, listener port, certificate | Terraform / cloud LB |
| Global anycast DNS failover | DNS provider |
| Which backup POP exists | GitOps infra |
| Permanent 80/20 capacity planning | Architecture RFC + Git |
| DDoS traffic scrubbing center switch | CDN vendor |

## Getting started (15 minutes)

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['edge']['prod']['steering']` with `steering`, `pools`, `guards`, `degradation`.
2. Replace `PRIMARY_WEIGHT` constant in your edge router with `LiveSteeringRouter`.
3. Map `pools.primary.endpoints` to your real origin hostnames.
4. Chaos: fail primary origin; set `primary_weight_pct: 60` live — confirm error rate drops without TF apply.
5. Add steering keys to origin degradation runbook.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [Data residency routing weights](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-regops-data-residency-routing-weights.md)
- [Origin timeout trees](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-edgeops-origin-timeout-trees.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — Terraform declares which origins exist; steering weights decide who serves traffic while primary bleeds.*