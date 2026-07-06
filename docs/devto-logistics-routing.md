---
title: "Retune Logistics Fleet Routing Parameters in Real Time (Kiponos Python SDK)"
published: true
tags: python, logistics, optimization, realtime
description: Change route cost weights, SLA penalties, and depot capacity limits in Python fleet optimizers while vehicles are on the road. Kiponos local reads, WebSocket deltas.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-logistics-routing.md
main_image: https://files.catbox.moe/2xo4es.jpg
---

Fleet routing optimizers balance **fuel cost**, **driver hours**, **customer SLA**, and **depot capacity**. A bridge closure at 4 PM should change weights immediately — not after batch jobs restart with new `config/routing.yaml`.

[Kiponos.io](https://kiponos.io) feeds Python routing workers live parameters: cost multipliers, max route duration, priority customer lists, and "avoid region" polygons metadata.

## Optimizer loop

```python
def solve_vrp(stops: list[Stop], kiponos) -> list[Route]:
    params = kiponos.path("routing", "fleet")
    cost = {
        "distance": params.get_float("weight_distance"),
        "time": params.get_float("weight_time"),
        "sla_penalty": params.get_float("weight_sla_breach"),
    }
    max_hours = params.get_float("max_driver_hours")
    avoid = params.get("avoid_regions_csv", "").split(",")
    return vrp_solver(stops, cost, max_hours, avoid)
```

Re-run or incremental solve picks up new weights on **next invocation** — local reads.

## Routing tree

```yaml
routing/
  fleet/
    weight_distance: 1.0
    weight_time: 1.4
    weight_sla_breach: 10.0
    max_driver_hours: 10.5
    avoid_regions_csv: ""
  depots/
    east/
      max_outbound_routes: 45
    west/
      max_outbound_routes: 38
  priority/
    customer_ids: acme-corp,globex-001
```

## Real-world scenarios

| Scenario | Live tweak |
|----------|------------|
| Highway closure | Add region to `avoid_regions_csv` |
| SLA crisis | Raise `weight_sla_breach` |
| Depot overload | Lower `max_outbound_routes` |
| VIP shipper | Update `priority/customer_ids` |

## Performance

VRP inner loops call config once per solve batch — `get_float()` is local.

## Getting started

1. [kiponos.io](https://kiponos.io) — `routing/fleet/*`
2. Connect Python worker with Kiponos SDK
3. Run dispatch; change weight in UI; re-solve same stops

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for Python logistics. Routes that adapt while trucks are rolling.*