---
title: "Cross-Service Handoff Signals and Locks in Real Time (Kiponos Python SDK)"
published: true
tags: python, microservices, architecture, realtime
description: Coordinate service handoffs with live flags and lease TTLs in a shared Kiponos tree. Python workers read locally; ops flips workflow state without redeploy.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-microservices-handoff.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-microservices-handoff.jpg
---

Microservices handoffs are where systems get fragile: "Payment captured — who tells shipping?" Teams bolt on **extra REST calls**, **SQS fan-out**, or **Redis locks** that each need their own config, monitoring, and deploy cycle.

[Kiponos.io](https://kiponos.io) offers a lighter pattern for **operational handoff state**: a shared live config tree that every Python service reads locally. Not your system of record — but the **coordination layer** for "ready to proceed," "hold," "escalate," and **short-lived lease locks**.

## The handoff problem

```python
def process_order(order_id: str) -> None:
    if not payment_captured(order_id):
        return  # poll again in 5s?
    if shipping_locked_by_other_worker(order_id):
        return
    ship(order_id)
```

Questions static config cannot answer mid-flight:

- How long should shipping **wait** after payment?
- When should we **escalate** to manual review?
- What is the **lease TTL** if a worker dies holding a lock?

Those values change during incidents — not during next week's release.

## Architecture

![Architecture diagram](https://files.catbox.moe/7c2gg7.png)

Payment sets `payment_captured: true`. Shipping reads it **from memory** before allocating a carrier label. Support watches `escalate_to_human` flip — no broadcast topic required for **slow-changing operational flags**.

## Handoff config tree

```yaml
workflow/
  handoffs/
    order-fulfillment/
      payment_captured: false
      fraud_cleared: false
      shipping_ready: false
      escalate_to_human: false
    timing/
      wait_after_payment_sec: 30
      max_handoff_retries: 5
      escalation_after_min: 15
    locks/
      shipping_lease_ttl_sec: 45
      allow_parallel_pick: false
```

## Python integration

```python
import os
from kiponos import Kiponos

os.environ["KIPONOS_PROFILE"] = "['orders']['v1']['prod']['workflow']"
kiponos = Kiponos.create_for_current_team()

def can_ship(order_id: str) -> bool:
    h = kiponos.path("workflow", "handoffs", "order-fulfillment")
    if not h.get_bool("payment_captured") or not h.get_bool("fraud_cleared"):
        return False
    if h.get_bool("escalate_to_human"):
        return False
    return acquire_lease("shipping", order_id,
        ttl=kiponos.path("workflow", "handoffs", "locks").get_int("shipping_lease_ttl_sec"))

def payment_completed(order_id: str) -> None:
    kiponos.path("workflow", "handoffs", "order-fulfillment").set("payment_captured", True)
```

**Reads** (`get_bool`) are local — call them every loop iteration. **Writes** (`set`) are infrequent state transitions — acceptable on the payment completion path.

Ops can override during an incident:

```python
# Dashboard: escalate_to_human = true
# Next can_ship() returns False — shipping stops without killing workers
```

## When to use Kiponos vs messaging

| Use Kiponos handoffs | Use Kafka/SQS instead |
|----------------------|------------------------|
| Slow-changing readiness flags | High-volume event streams |
| Human-operable overrides | Strict event ordering audit |
| Cross-team tunable TTLs | Payload-heavy domain events |
| "Is it safe to proceed?" gates | "Something happened" notifications |

Kiponos complements messaging — it removes **config-driven polling intervals** and **magic timeout constants** from handoff code.

## Real-world scenarios

| Scenario | Live tweak |
|----------|------------|
| Fraud review backlog | Set `escalate_to_human: true`, pause shipping |
| Payment partner slow | Increase `wait_after_payment_sec` |
| Worker storm on shipping | Lower `shipping_lease_ttl_sec`, disable `allow_parallel_pick` |
| Black Friday | Tighten `escalation_after_min` for stuck orders |

## Performance

- Handoff checks use **cached tree lookups** — not HTTP to a lock service per order
- WebSocket applies deltas in the background
- Lease logic stays in your code; **TTL values** come from Kiponos live

## Compare to alternatives

| Approach | Ops can intervene mid-flight | Read cost per check |
|----------|------------------------------|---------------------|
| Hard-coded sleeps | No | Zero |
| Poll Redis/DB | Yes | Network RTT |
| Workflow engine only | Vendor UI | Engine-specific |
| **Kiponos + your workers** | **Dashboard + SDK** | **Zero (local)** |

## Getting started

1. [Free TeamPro at kiponos.io](https://kiponos.io) — profile `workflow/handoffs/*`
2. Connect Python services with `KIPONOS_ID`, `KIPONOS_ACCESS`, profile path
3. Replace magic sleeps and env-based TTLs with `kiponos.path(...).get_*()`
4. Run payment → shipping flow; flip `escalate_to_human` in UI; confirm shipping stops instantly

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

## What is next

Combine handoffs with **saga timeouts** and **event topic routing** in the same tree — one live control plane for distributed order fulfillment.

---

*Kiponos.io — real-time config for Python. Hand off work across services without redeploying coordination logic.*