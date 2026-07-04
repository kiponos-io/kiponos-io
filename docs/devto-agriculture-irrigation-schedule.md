---
title: "Adjust Irrigation Moisture Triggers During a Drought Warning — No Python Worker Restart (Kiponos SDK)"
published: false
tags: python, agriculture, iot, sustainability
description: Change soil moisture thresholds, zone schedules, and pump duty caps in Python irrigation controllers while pivots keep running. Kiponos local reads on every sensor evaluation loop.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-agriculture-irrigation-schedule.md
main_image: https://litter.catbox.moe/em0tjj.jpg
---

County drought advisory level **2** issued Thursday. Your almond blocks in sector 7 still irrigate when soil moisture drops below `0.32` — a constant in `irrigation_policy.py` calibrated for a normal rainfall year.

The farm manager radios the ops center:

> "Cut **pump hours 30%** on sectors 5–9 and raise the moisture trigger to **0.38** until the advisory lifts — we cannot redeploy controllers mid-harvest."

The sensor mesh is healthy. The policy constants are not. Every hour of frozen thresholds wastes water you no longer have rights to pump.

**`SOIL_MOISTURE_TRIGGER` is not agronomy gospel — it is this week's water budget dial.**

[Kiponos.io](https://kiponos.io) feeds Python irrigation workers live thresholds — WebSocket deltas, in-memory reads on every control loop.

## The problem: module constants on the irrigation hot path

```python
# irrigation_policy.py — set during spring commissioning
SOIL_MOISTURE_TRIGGER = 0.32
MAX_PUMP_HOURS_PER_DAY = 10
ZONE_COOLDOWN_MINUTES = 45

def should_irrigate(zone: Zone, reading: SensorReading) -> bool:
    if zone.pump_hours_today() >= MAX_PUMP_HOURS_PER_DAY:
        return False
    return reading.soil_moisture() < SOIL_MOISTURE_TRIGGER
```

Problems:

1. **Redeploy edge workers** — while soil dries and trees stress
2. **Per-sector env files** — drought response needs minute-level coordination
3. **Poll cloud config API** — adds RTT inside tight sensor evaluation loops

| What teams say | What production does |
|----------------|---------------------|
| "Moisture targets come from crop consultants annually" | Water board advisories change weekly |
| "IoT platform has a rules UI" | Your Python orchestrator still owns the hot loop |
| "We'll manually override pumps" | 400 zones do not scale to phone calls |
| "Rain forecast will save us" | Forecasts do not unpause a frozen constant |

## The Aha: moisture triggers and pump caps are operational water policy

Store irrigation policy under `irrigation/zones` in Kiponos. Each `should_irrigate()` reads sector-specific `soil_moisture_trigger`, `max_pump_hours_per_day`, and drought mode flags from the in-memory tree. When the farm manager raises sector 7's trigger to `0.38`, the **next** sensor evaluation uses it — no worker restart.

## What is Kiponos.io — for Python irrigation controllers

[Kiponos.io](https://kiponos.io) is a config hub with Java and Python SDKs. `Kiponos.create_for_current_team()` connects over WebSocket, hydrates the tree for a profile like `['agriculture']['prod']['irrigation']`, and serves **local** `get_float()` / `get_int()` on the hot path.

Updates are **async deltas** — changing `soil_moisture_trigger` patches one key in memory. Your sensor loop never blocks on the network waiting for config.

`after_value_changed` logs drought policy flips or signals MQTT bridge to broadcast schedule revisions to field displays.

## Architecture

![Architecture diagram](https://litter.catbox.moe/eheoj8.png)

## Example config tree

```yaml
irrigation/
  global/
    drought_mode: false
    county_advisory_level: 0
    halt_all_pumping: false
  zones/
    sector_07/
      soil_moisture_trigger: 0.32
      max_pump_hours_per_day: 10
      zone_cooldown_minutes: 45
      crop: almond
    sector_05/
      soil_moisture_trigger: 0.30
      max_pump_hours_per_day: 12
      zone_cooldown_minutes: 40
  drought/
    level_2/
      moisture_trigger_bump: 0.06
      pump_hours_multiplier: 0.70
      priority_zones_csv: sector_07,sector_08
  schedules/
    night_irrigation_only: false
    start_hour_utc: 6
    end_hour_utc: 18
```

## Python integration (irrigation control loop)

```python
import logging
from kiponos import Kiponos

log = logging.getLogger(__name__)

kiponos = Kiponos.create_for_current_team()
# Profile: ['agriculture']['prod']['irrigation'] via KIPONOS_PROFILE env


def _zone_cfg(zone_id: str):
    return kiponos.path("irrigation", "zones", zone_id)


def effective_moisture_trigger(zone_id: str) -> float:
    cfg = _zone_cfg(zone_id)
    base = cfg.get_float("soil_moisture_trigger", 0.32)
    drought = kiponos.path("irrigation", "global")
    if drought.get_bool("drought_mode", False):
        level = drought.get_int("county_advisory_level", 0)
        if level >= 2:
            bump = kiponos.path("irrigation", "drought", "level_2").get_float("moisture_trigger_bump", 0.06)
            return base + bump
    return base


def effective_max_pump_hours(zone_id: str) -> float:
    cfg = _zone_cfg(zone_id)
    base = cfg.get_float("max_pump_hours_per_day", 10.0)
    if kiponos.path("irrigation", "global").get_bool("drought_mode", False):
        mult = kiponos.path("irrigation", "drought", "level_2").get_float("pump_hours_multiplier", 1.0)
        return base * mult
    return base


def should_irrigate(zone: Zone, reading: SensorReading) -> bool:
    if kiponos.path("irrigation", "global").get_bool("halt_all_pumping", False):
        return False

    if zone.pump_hours_today() >= effective_max_pump_hours(zone.id()):
        return False

    trigger = effective_moisture_trigger(zone.id())
    cooldown = _zone_cfg(zone.id()).get_int("zone_cooldown_minutes", 45)
    if zone.minutes_since_last_run() < cooldown:
        return False

    return reading.soil_moisture() < trigger


kiponos.after_value_changed(
    lambda change: log.info("Irrigation policy changed: %s → %s", change.path, change.new_value)
    if change.path.startswith("irrigation/")
    else None
)
```

Every `get_float()` is a **local memory read** — safe inside loops that evaluate hundreds of zones every few minutes.

## Real scenarios

| Event | `SOIL_MOISTURE_TRIGGER = 0.32` folklore | Kiponos path |
|-------|----------------------------------------|--------------|
| County drought level 2 | Redeploy edge workers | `irrigation/global/drought_mode: true` + level_2 bumps |
| Priority orchard block | Per-zone env file | `irrigation/zones/sector_07/soil_moisture_trigger: 0.38` |
| Pump license curfew | Manual operator shifts | Enable `irrigation/schedules/night_irrigation_only` |
| Water board emergency stop | Phone tree to foremen | `irrigation/global/halt_all_pumping: true` |
| Advisory lifted | Forgotten constant revert | Ops clears `drought_mode` with dashboard audit |

## Performance — why sensor loops stay fast

- One WebSocket per irrigation worker — not one config fetch per zone evaluation
- `get_float("soil_moisture_trigger")` is O(1) on the cached tree
- Delta updates — sector trigger change sends one patch
- No process restart — asyncio/Celery workers keep consuming sensor stream
- `after_value_changed` on drought toggles — lightweight MQTT notify, not per-reading

## Compare to alternatives

| Approach | Raise moisture trigger during drought | Per-evaluation read cost |
|----------|--------------------------------------|--------------------------|
| Module constant in `irrigation_policy.py` | Redeploy workers | Zero (frozen) |
| Per-zone `.env` files | SSH to edge devices | Zero after manual edit |
| Poll cloud IoT rules API | Possible | Network RTT × zone count |
| **Kiponos SDK** | **Dashboard (seconds)** | **Memory read** |

## When not to use Kiponos for irrigation

| Case | Better approach |
|------|-----------------|
| Soil sensor calibration coefficients | Device firmware / commissioning |
| Water rights legal allotments | Regulatory system of record |
| Replacing control with ML evapotranspiration model | Offline agronomy pipeline |
| Pump hardware safety interlocks | PLC firmware — not ops floats |

## Getting started (15 minutes)

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['agriculture']['prod']['irrigation']`.
2. `pip install kiponos` — set `KIPONOS_ID`, `KIPONOS_ACCESS`, `KIPONOS_PROFILE`.
3. Create `irrigation/zones/sector_07` with `soil_moisture_trigger`, `max_pump_hours_per_day`.
4. Replace module-level `SOIL_MOISTURE_TRIGGER` with `effective_moisture_trigger(zone_id)`.
5. Game day: simulate dry readings in staging, enable `drought_mode` live, re-evaluate zone — irrigation threshold shifts **without worker restart**.

**Further reading:**

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for Python agriculture. Water smarter while the pivots keep turning.*