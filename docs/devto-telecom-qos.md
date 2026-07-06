---
title: "Steer Telecom QoS and Bandwidth Routing in Real Time (Kiponos Java SDK)"
published: true
tags: java, telecom, networking, realtime
description: Live QoS class weights, bandwidth caps, and congestion policies in Java network control planes. NOC edits Kiponos; edge controllers read locally via WebSocket deltas.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-telecom-qos.md
main_image: https://files.catbox.moe/tl9vrp.jpg
---

Congestion is measured in seconds. NOC needs to **elevate voice**, **cap bulk transfers**, and **shift peering preference** while Java control-plane services push policies to thousands of edge elements. Static policy files mean minutes-to-hours propagation.

[Kiponos.io](https://kiponos.io) holds QoS class weights and bandwidth policies in a live tree — control services read locally, apply to southbound APIs on their schedule.

## Policy evaluation

```java
public QosPolicy effectivePolicy(String subscriberClass) {
    var qos = kiponos.path("qos", "classes");
    var limits = kiponos.path("qos", "limits");
    return QosPolicy.builder()
        .voiceWeight(qos.getInt("voice_weight"))
        .videoWeight(qos.getInt("video_weight"))
        .bulkCapMbps(limits.getInt("bulk_cap_mbps"))
        .degradedMode(kiponos.path("qos", "modes").getBool("congestion_active"))
        .build();
}
```

## QoS tree

```yaml
qos/
  classes/
    voice_weight: 10
    video_weight: 6
    best_effort_weight: 2
  limits/
    bulk_cap_mbps: 50
    per_subscriber_max_mbps: 200
  modes/
    congestion_active: false
    prefer_peering_a: true
  regions/
    east/
      bulk_cap_mbps: 40
```

## Real-world scenarios

| Scenario | Live tweak |
|----------|------------|
| Backbone congestion | `congestion_active: true`, raise `voice_weight` |
| DDoS bulk flood | Lower `bulk_cap_mbps` |
| Peering outage | Flip `prefer_peering_a` |
| Stadium event | Regional override under `regions/east` |

## Performance

Policy pulls happen per control tick — not per packet — but still benefit from **local cache**.

## Getting started

1. [kiponos.io](https://kiponos.io) — `qos/*`
2. Wire Java control service reads
3. Simulate congestion; adjust weights live; verify southbound API payloads

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for Java telecom. QoS that responds at NOC speed.*