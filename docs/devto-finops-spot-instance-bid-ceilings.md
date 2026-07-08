---
title: "Spot Instance Bid Ceilings Live — Cap Interrupt Risk Without Terraform (Python SDK)"
published: false
tags: python, finops, devops, architecture
description: Spot bid max in launch templates is infra state — not ops posture. Kiponos holds bid ceilings and fallback flags for batch workers.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-finops-spot-instance-bid-ceilings.md
main_image: https://files.catbox.moe/n1iaiq.jpg
---

Thursday 06:18 UTC. PagerDuty wakes **batch-platform** on-call **Nina Kowalski**: **Spot interruption rate** on the `ml-train` fleet hit **38%** in the last hour — three times the weekly baseline. EC2 Fleet events show capacity crunch in `us-east-1a`; jobs are restarting in loops and **burning scheduler slots**.

FinOps engineer **Leo Martins** joins the bridge with data platform lead **Hannah Cho**. Hannah does not want to kill the nightly ETL — it feeds downstream dashboards by 09:00. Leo needs to **lower spot bid aggression** and **enable on-demand fallback** for the training queue without a Terraform apply:

> "Drop `max_bid_multiplier` from **1.8** to **1.2** and flip on-demand fallback for `queue_ml_train`. I am not waiting for a launch template version bump while jobs restart every four minutes."

The batch worker supervisor still reads `MAX_BID_MULTIPLIER = 1.8` from `spot_policy.py`, compiled into the **batch-spot-broker** Celery workers last Tuesday. Terraform launch templates have `spot_max_price_percentage_over_lowest_price = 180` — infra state, not an incident knob.

The platform director asks:

> "We already choose bid price on every fleet request. Why does **interrupt storm response** require a **Terraform apply** when the knob is a float?"

Most Python batch fleets treat spot bid ceilings as **infra archaeology**: Terraform launch templates, ASG mixed-instance policies, and a module constant that only changes after worker recycle. [Kiponos.io](https://kiponos.io) collapses `max_bid_multiplier`, per-queue overrides, and on-demand fallback flags into **one operational tree** — readable on every fleet allocation with local `get*()` calls and adjustable from the dashboard while workers keep running.

## The problem — max_bid_multiplier baked into static config

A typical batch spot broker allocates capacity like this:

```python
# spot_policy.py — imported once at worker boot
MAX_BID_MULTIPLIER = 1.8
ON_DEMAND_FALLBACK_ENABLED = False

def compute_spot_bid(on_demand_price: float) -> float:
    return on_demand_price * MAX_BID_MULTIPLIER

async def request_fleet(queue: str, instance_type: str) -> FleetAllocation:
    bid = compute_spot_bid(await pricing.on_demand(instance_type))
    return await ec2.request_spot_fleet(bid=bid, instance_type=instance_type)
```

Bid policy usually lives elsewhere — scattered and deploy-bound:

```hcl
# terraform/batch-spot/main.tf — apply cycle, not incident knob
spot_max_price_percentage_over_lowest_price = 180
on_demand_percentage_above_base_capacity   = 0
```

Or worse — broker constant and Terraform disagree:

```python
# Worker says 1.8; Terraform says 150%; interruption storm at 06:00
MAX_BID_MULTIPLIER = 1.8
```

The allocation loop runs **every few seconds per pending job**. During an interruption wave you need to:

1. Lower **`queues/ml_train/max_bid_multiplier`** to reduce outbid risk without killing all spot savings
2. Flip **`queues/ml_train/on_demand_fallback_enabled`** so critical jobs complete before 09:00
3. Raise **`posture/interrupt_shed_enabled`** to pause non-critical queues during the storm

Doing that through Terraform while jobs restart in loops is not FinOps — it is **interrupt theater with SLA interest**.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Launch template max price is the ceiling" | Custom brokers often apply a **second** multiplier in Python |
| "Spot savings are worth any interrupt rate" | Interrupt storms waste scheduler slots and **increase** total compute cost |
| "We'll switch to on-demand in Terraform" | `terraform apply` during a 06:00 incident is not ops — it is archaeology |
| "Fleet diversification fixes interrupts" | Diversification helps; **bid posture** still needs mid-storm tuning |
| "Batch workers read instance metadata" | Metadata tells you interrupted; it does not lower your bid |

## The Aha

**max_bid_multiplier is operational config** — it changes during interruption waves, capacity crunches, and SLA incidents. It belongs in a **live tree** the spot broker already reads with `get_float()`, not in a constant imported at worker boot.

## What Kiponos.io is for spot bid ceilings

[Kiponos.io](https://kiponos.io) is a real-time configuration hub with Java and Python SDKs. `Kiponos.create_for_current_team()` connects over WebSocket; the profile tree — for example `['batch']['prod']['spot']` — hydrates into **in-process memory** at worker startup.

When Leo sets `queues/ml_train/max_bid_multiplier` to `1.2`, a **delta** patches only that key. The next `kiponos.path("queues", queue).get_float("max_bid_multiplier")` on a fleet request is a **local memory read** — no HTTP to a config API, no poll loop, no Terraform state lock.

`after_value_changed` logs bid flips and can emit `spot_bid_ceiling_changed` metrics to your FinOps dashboard **without** restarting Celery workers.

No restart. No redeploy. No recycling the worker pool.

Honest boundary: Kiponos does **not** replace Terraform for launch templates, IAM for fleet roles, or your cloud provider's spot pricing API. It owns **operational bid posture** Python brokers read on every allocation decision.

## Architecture

![Architecture diagram](https://files.catbox.moe/mkj0il.png)

**Terraform documents baseline fleet templates; authoritative incident bid ceilings live in Kiponos** where lowering them takes seconds.

## Config tree — defaults, queues, posture, regions, and audit

Five folders — `defaults`, `queues`, `posture`, `regions`, `audit`:

```yaml
defaults/
  max_bid_multiplier: 1.5
  on_demand_fallback_enabled: false
  min_bid_multiplier: 1.0
  max_bid_multiplier_ceiling: 2.5
queues/
  ml_train/
    max_bid_multiplier: 1.8
    on_demand_fallback_enabled: false
    enabled: true
    priority: high
  etl_nightly/
    max_bid_multiplier: 1.4
    on_demand_fallback_enabled: false
    enabled: true
    priority: medium
  adhoc_analytics/
    max_bid_multiplier: 1.2
    on_demand_fallback_enabled: false
    enabled: true
    priority: low
posture/
  interrupt_shed_enabled: false
  shed_queues: ["adhoc_analytics"]
  shed_message: "Spot interruption storm — non-critical queues paused"
regions/
  us-east-1/
    capacity_risk_high: false
    bid_discount_factor: 1.0
  us-west-2/
    capacity_risk_high: false
    bid_discount_factor: 1.0
audit/
  last_bid_change_by: ""
  last_bid_change_at_ms: 0
  emit_interrupt_metrics: true
```

One tree. One profile path: `['batch']['prod']['spot']`. Staging interrupt drills share **identical key layout** — only values differ.

## Python integration — per-queue bid ceiling + on-demand fallback

```python
import logging
from celery import Celery
from kiponos import Kiponos

log = logging.getLogger(__name__)
app = Celery("batch-spot-broker")

kiponos = Kiponos.create_for_current_team()
# Profile: ['batch']['prod']['spot'] via KIPONOS_PROFILE env

def max_bid_multiplier(queue: str, region: str) -> float:
    defaults = kiponos.path("spot", "defaults")
    base = defaults.get_float("max_bid_multiplier", 1.5)

    queue_cfg = kiponos.path("spot", "queues", queue)
    if queue_cfg.exists() and queue_cfg.get_bool("enabled", True):
        base = queue_cfg.get_float("max_bid_multiplier", base)

    region_cfg = kiponos.path("spot", "regions", region)
    if region_cfg.get_bool("capacity_risk_high", False):
        factor = region_cfg.get_float("bid_discount_factor", 1.0)
        base = max(base * factor, defaults.get_float("min_bid_multiplier", 1.0))

    ceiling = defaults.get_float("max_bid_multiplier_ceiling", 2.5)
    return min(base, ceiling)

def on_demand_fallback_enabled(queue: str) -> bool:
    queue_cfg = kiponos.path("spot", "queues", queue)
    if queue_cfg.exists():
        return queue_cfg.get_bool("on_demand_fallback_enabled", False)
    return kiponos.path("spot", "defaults").get_bool("on_demand_fallback_enabled", False)

kiponos.after_value_changed(
    lambda change: log.info("Spot bid delta: path=%s value=%s", change.path, change.new_value)
)

@app.task
async def allocate_fleet(queue: str, region: str, instance_type: str) -> dict:
    posture = kiponos.path("spot", "posture")
    if posture.get_bool("interrupt_shed_enabled", False):
        if queue in posture.get_list("shed_queues", []):
            return {"status": "shed", "message": posture.get("shed_message", "paused")}

    on_demand_price = await pricing.on_demand(region, instance_type)
    multiplier = max_bid_multiplier(queue, region)
    bid = on_demand_price * multiplier

    if on_demand_fallback_enabled(queue):
        allocation = await ec2.request_mixed_fleet(
            spot_bid=bid,
            on_demand_percentage=50,
            instance_type=instance_type,
            region=region,
        )
    else:
        allocation = await ec2.request_spot_fleet(
            bid=bid,
            instance_type=instance_type,
            region=region,
        )

    if kiponos.path("spot", "audit").get_bool("emit_interrupt_metrics", True):
        metrics.record("spot_bid_multiplier", multiplier, queue=queue, region=region)

    return {"status": "allocated", "bid": bid, "fleet_id": allocation.id}

@app.task
def on_spot_interruption(event: dict) -> None:
    region = event["region"]
    kiponos.path("spot", "regions", region).set("capacity_risk_high", True)
    metrics.inc("spot_interruption", region=region)
```

Every `get_float()`, `get_bool()`, and `get_list()` on the allocation path is **O(1) local cache** — microseconds, not cross-region config service RTT.

**Launch templates and IAM roles** stay in Terraform — Kiponos owns the **bid multipliers** that change when interruption alarms fire.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| Spot interruption wave — lower bid ceiling and enable on-demand fallback | Terraform apply + launch template version bump | Dashboard: `queues/ml_train/max_bid_multiplier: 1.2` + fallback live |
| SLA-critical ETL deadline | Manual on-demand ASG spin-up | `queues/etl_nightly/on_demand_fallback_enabled: true` |
| FinOps cost-saver window | Raise Terraform spot percentage — risky | Lower multipliers on `adhoc_analytics` only |
| Regional capacity crunch | Broker keeps aggressive 1.8× bids | `regions/us-east-1/capacity_risk_high: true` applies discount factor |
| Post-storm restore | Second Terraform apply | Reset `queues` and `posture` subtree in dashboard |

## Performance — hot path on fleet allocation decisions

- **Bid multiplier per allocation** — three local reads (defaults, queue, region); no HTTP on fleet path
- **Per-queue nesting** — ml_train, etl_nightly, adhoc each get a folder; no env var matrix
- **Delta updates** — lowering one queue multiplier sends one patch; other queues unchanged
- **Interrupt shed flip** — one boolean pauses low-priority queues; no Celery code deploy
- **One WebSocket per worker** — background sync; allocation loop never blocks on config API RTT
- **Complements Terraform** — templates own baseline; broker owns **incident bid posture**

## Compare to alternatives

| Approach | Mid-storm bid lower | Per-queue fallback | Interrupt shed |
|----------|--------------------|--------------------|----------------|
| Terraform launch template | Poor — apply + rollout | Awkward — per ASG | Manual ASG suspend |
| EC2 Fleet API only | No runtime multiplier | API flags per call — not centralized | Custom scripts |
| Redis hash of bids | Extra RTT or stale cache | Medium — key sprawl | Separate flag keys |
| Hard-coded spot_policy.py | Poor — worker recycle | Poor — redeploy | Code change |
| Spreadsheet + human | N/A | Humans edit; broker unchanged | Bridge chaos |
| **Kiponos live hub** | **Seconds — dashboard delta** | **Per-queue subtree** | **One posture boolean** |

## When not to use Kiponos

| Case | Use instead |
|------|-------------|
| Launch template AMI IDs and instance profiles | Terraform / CloudFormation |
| IAM roles and fleet service-linked roles | Cloud IAM — Git-reviewed |
| Spot instance interruption behavior at hypervisor | AWS — not configurable via app tree |
| Immutable cloud invoice line items | Billing warehouse — not live config |
| One-time bootstrap bid from capacity planning | Terraform at fleet create time |

## Getting started (15 minutes)

1. Sign up at [kiponos.io](https://kiponos.io) (TeamPro).
2. Create profile path `['batch']['prod']['spot']`.
3. Add `defaults/max_bid_multiplier`, `queues/ml_train/max_bid_multiplier`, and wire `max_bid_multiplier()` in your fleet allocation path.
4. `celery -A batch_spot_broker worker` — confirm log shows WebSocket handshake.
5. Lower `queues/ml_train/max_bid_multiplier` in dashboard; watch next fleet request use lower bid **without** worker restart.
6. Drill: enable `queues/ml_train/on_demand_fallback_enabled` and `posture/interrupt_shed_enabled` in staging.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [GPU dollars per request](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-finops-gpu-dollars-per-request.md)
- [Autoscale guardrails live](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-finops-autoscale-guardrails-live.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*max_bid_multiplier belongs in the live ops tree — not in constants that mock your batch SRE during the next spot interruption wave.*