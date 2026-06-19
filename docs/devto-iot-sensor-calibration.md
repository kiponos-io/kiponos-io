---
title: "Push IoT Sensor Thresholds to Thousands of Edge Collectors in Real Time (Kiponos Python SDK)"
published: true
tags: python, iot, edge, realtime
description: Calibrate alert thresholds and sampling rates across an IoT fleet without redeploying edge agents. Kiponos Python SDK reads live values locally with zero latency.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-iot-sensor-calibration.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-iot-sensors.jpg
---

A heatwave shifts baseline temperatures. A factory line change alters vibration norms. You need **new alert thresholds** on 10,000 edge collectors **today** — not after an OTA firmware cycle.

[Kiponos.io](https://kiponos.io) connects Python edge agents over WebSocket. Thresholds live in local memory; ops pushes **delta updates** from the hub.

## Edge collector hot loop

```python
def evaluate(sensor_id: str, reading: float, kiponos) -> str:
    cfg = kiponos.path("sensors", sensor_id)
    high = cfg.get_float("alert_high")
    low = cfg.get_float("alert_low")
    if reading > high:
        return "ALERT_HIGH"
    if reading < low:
        return "ALERT_LOW"
    return "OK"
```

Called on every sample batch. Reads must be **local** — no cloud round-trip per reading.

## Fleet config tree

```
sensors/
  boiler_12/
    alert_high: 92.5
    alert_low: 18.0
    sample_interval_ms: 1000
  conveyor_7/
    alert_high: 4.2
    alert_low: 0.1
    sample_interval_ms: 500
defaults/
  alert_high: 100.0
  sample_interval_ms: 2000
```

## Ops workflows

| Task | Kiponos action |
|------|----------------|
| Heat wave | Raise `alert_high` for outdoor sensors |
| New equipment | Clone folder, tune thresholds live |
| Reduce uplink cost | Increase `sample_interval_ms` fleet-wide |
| Incident | Tighten thresholds on affected segment |

Each edge agent runs one Kiponos SDK connection. Dashboard change → delta patch → all connected agents update in memory.

## Why not poll a central API?

Polling adds latency, battery cost on edge, and failure modes when the network blips. Kiponos keeps **last known good values** locally and syncs deltas when connected.

Start at [kiponos.io](https://kiponos.io). Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for Python. Calibrate the fleet while it runs.*