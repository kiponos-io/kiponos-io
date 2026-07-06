---
title: "Microservices That Collaborate in Real Time via a Shared Kiponos Config Tree (Java SDK)"
published: true
tags: java, microservices, architecture, realtime
description: Services coordinate through a live shared config hub — handoff flags, capacity signals, and workflow state with zero-latency local reads. No inter-service config polling.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-microservices-collaboration.md
main_image: https://files.catbox.moe/xvnh3m.jpg
---

Microservices are sold as independent. Operationally they are **tightly coupled** — inventory waits on payment, shipping waits on fraud, support waits on everyone. Teams coordinate with Slack, ad-hoc REST calls, and "just check Redis" hacks that become production dependencies.

[Kiponos.io](https://kiponos.io) is a different coordination layer: a **shared live config tree** every service reads locally. Not your domain database — the **operational collaboration surface** for readiness flags, capacity signals, degradation modes, and human overrides.

## What Kiponos is (and is not)

**Kiponos is:** a real-time config hub. Connected SDKs hold the latest values **in memory**, updated over WebSocket **delta patches**. Reads are local (`kiponos.path(...).getBool()`). Writes propagate to all observers.

**Kiponos is not:** an event store, workflow engine, or source of truth for customer data. Use it for **tunable operational state** every service must agree on *right now*.

## The anti-pattern: chatty coordination

```java
// Every service calling every other service for "are we ready?"
if (rest.get("http://payment/ready").boolean()) { ... }
if (rest.get("http://inventory/capacity").double() < 0.9) { ... }
```

Under load this becomes **N×M HTTP chatter**, brittle caches, and config duplicated in five YAML files.

## Collaboration via shared tree

![Architecture diagram](https://files.catbox.moe/bwx72e.png)

## Example tree: order fulfillment

```yaml
workflow/
  collab/
    order-fulfillment/
      inventory_reserved: false
      payment_captured: false
      fraud_review_required: false
      shipping_ready: false
    capacity/
      warehouse_east_utilization: 0.72
      carrier_api_status: healthy
    modes/
      degrade_to_standard_shipping: false
      manual_ops_override: false
```

## Java: read locally, write on state change

**Inventory service** — after reserve:

```java
kiponos.path("workflow", "collab", "order-fulfillment")
    .set("inventory_reserved", true);
```

**Payment service** — before capture:

```java
var collab = kiponos.path("workflow", "collab", "order-fulfillment");
if (!collab.getBool("inventory_reserved")) {
    return PaymentResult.hold("awaiting_inventory");
}
if (collab.getBool("fraud_review_required")) {
    return PaymentResult.manualReview();
}
```

**Shipping service** — poll loop (local reads only):

```java
var collab = kiponos.path("workflow", "collab", "order-fulfillment");
var cap = kiponos.path("workflow", "collab", "capacity");
if (collab.getBool("shipping_ready")
    && "healthy".equals(cap.get("carrier_api_status"))) {
    dispatch(label);
}
```

Every `getBool()` is a **memory read** — safe in tight loops. Writes happen on **state transitions**, not per HTTP request.

## Human and ops in the same control plane

Support lead sets `manual_ops_override: true` in the Kiponos dashboard during a carrier outage. All services see it on the next read — no paging every team to edit their own config.

| Actor | Typical write |
|-------|----------------|
| Inventory service | `inventory_reserved` |
| Payment service | `payment_captured` |
| Fraud service | `fraud_review_required` |
| Ops / support | `degrade_to_standard_shipping`, `manual_ops_override` |

## Performance characteristics

| Operation | Frequency | Cost |
|-----------|-----------|------|
| `getBool()` in request path | High | Local cache O(1) |
| `set()` on state transition | Low | WebSocket write ~ms |
| Dashboard edit | Rare | Delta to all SDKs |

Collaboration flags change **orders of magnitude** less often than business events — perfect fit for a live config hub.

## Compare to alternatives

| Approach | Shared visibility | Mid-incident override | Read latency |
|----------|-------------------|----------------------|--------------|
| REST "status" endpoints | Per-service | Painful | HTTP RTT |
| Shared Redis keys | Good | Custom tooling | Redis RTT |
| Static YAML | Poor | Redeploy fleet | Zero |
| **Kiponos tree** | **Single UI** | **Dashboard** | **Zero (local)** |

## Getting started

1. [Free TeamPro at kiponos.io](https://kiponos.io) — one profile `workflow/collab/*`
2. Add SDK to each participating service with the **same** profile path
3. Move cross-service readiness checks from HTTP to `kiponos.path(...).get*()`
4. Run an end-to-end order; flip `manual_ops_override` in UI; watch all services react

Open-source skill + golden Java: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

## What is next

Layer **saga timeouts**, **event routing**, and **handoff lease TTLs** in the same profile — a full operational control plane for microservices without multiplying config systems.

---

*Kiponos.io — real-time config for Java. Microservices collaborate through one live tree, not a mesh of status endpoints.*