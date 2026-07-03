---
title: "Adjust Airline Overbooking Coefficients During Irregular Ops — No Java Restart (Kiponos SDK)"
published: false
tags: java, travel, aviation, operations
description: Change overbooking ratios, standby caps, and route-specific inventory buffers in Java revenue management services while bookings continue. Kiponos local reads on every availability check.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-travel-overbooking-limits.md
main_image: https://files.catbox.moe/2xo4es.jpg
---

Nor'easter day 2. **340 flights** cancelled across your East Coast hub. No-show rates collapse — everyone who still has a seat shows up. Your availability service still sells against `overbook_ratio: 1.08` baked into `inventory-policy.yml` since the summer schedule publish.

The network operations controller calls revenue management:

> "We need to **stop overselling** JFK–BOS tonight — people are getting bumped with hotel vouchers we cannot staff."

Engineering's playbook is a config change, regression suite, and rolling restart of inventory pods during the busiest rebooking window. Every minute of frozen overbooking policy compounds gate chaos.

**`overbook_ratio` is not architecture. It is how much inventory risk you accept during tonight's irregular ops.**

[Kiponos.io](https://kiponos.io) lets revenue and ops teams move overbooking limits **while the booking engine keeps running** — WebSocket deltas, in-memory reads on every availability evaluation.

## The problem: static ratios on the availability hot path

```java
@Service
public class LegacyAvailabilityService {
    @Value("${inventory.overbook_ratio:1.08}")
    private double overbookRatio;

    @Value("${inventory.max_standby:15}")
    private int maxStandby;

    public SeatAvailability check(FlightLeg leg, int confirmedPax) {
        int effectiveCapacity = (int) (leg.physicalSeats() * overbookRatio);
        int remaining = effectiveCapacity - confirmedPax;
        return new SeatAvailability(remaining, maxStandby);
    }
}
```

Those coefficients usually come from:

1. **YAML at seasonal schedule publish** — weather does not wait for the next season file
2. **Stored procedure batch** — RM analysts cannot move a float during a meltdown
3. **Emergency feature branch** — CI queue while gate agents issue vouchers

| What teams say | What production does |
|----------------|---------------------|
| "Overbooking is a revenue science model" | Irregular ops need **manual guardrails** in minutes |
| "We tune ratios in the RM workstation overnight" | Booking API still reads JVM constants until restart |
| "IROP playbook is a PDF" | PDFs do not patch running services |
| "Caps are legal/compliance" | Ops caps are **operational**, distinct from regulatory minimums |

## The Aha: overbooking coefficients are tonight's risk dial

Store inventory policy under `inventory/overbook` in Kiponos. Each `check()` reads route-specific `overbook_ratio`, `max_standby`, and IROP overrides from the in-memory tree. When ops sets JFK hub ratio to `1.00`, the **next** availability call honors it — no pod restart.

## What is Kiponos.io — for travel inventory

Kiponos connects your Spring Boot availability service to a live config tree. Profile `['travel']['prod']['inventory']` hydrates at startup. Dashboard edits are **WebSocket deltas**. `kiponos.path("inventory", "overbook", route.code()).getFloat("overbook_ratio")` is a **local read** — no JDBC on every search and hold request during rebooking surges.

Keep **fare rules and cabin mapping** in code and RM systems. Move **operational oversell floats** to the hub where network controllers and analysts collaborate during IROPs.

## Architecture

![Architecture diagram](https://litter.catbox.moe/r0lym1.png)

## Example config tree

```yaml
inventory/
  overbook/
    default/
      overbook_ratio: 1.08
      max_standby: 15
      bump_risk_threshold: 0.92
    routes/
      JFK_BOS/
        overbook_ratio: 1.00
        max_standby: 5
        irop_mode: true
      LAX_SFO/
        overbook_ratio: 1.05
        max_standby: 10
  irop/
    global_oversell_halt: false
    hub_jfk_strict: true
    voucher_budget_alert_usd: 50000
  standby/
    enabled: true
    priority_crew_only: false
```

## Bootstrap and integration (Spring Boot 3)

```java
@Configuration
public class KiponosConfig {

    @Bean
    public Kiponos kiponos(
            @Value("${kiponos.team-id}") String teamId,
            @Value("${kiponos.access-key}") String accessKey,
            @Value("${kiponos.profile-path}") String profilePath) {
        return Kiponos.builder()
                .teamId(teamId)
                .accessKey(accessKey)
                .profilePath(profilePath)
                .build();
    }
}
```

```java
@Service
public class KiponosAvailabilityService {

    private final Kiponos kiponos;

    public KiponosAvailabilityService(Kiponos kiponos) {
        this.kiponos = kiponos;
        kiponos.afterValueChanged(change -> {
            if (change.path().startsWith("inventory/irop")) {
                log.warn("IROP inventory policy: {} → {}", change.path(), change.newValue());
            }
        });
    }

    public SeatAvailability check(FlightLeg leg, int confirmedPax) {
        if (kiponos.path("inventory", "irop").getBool("global_oversell_halt", false)) {
            return SeatAvailability.strict(leg.physicalSeats() - confirmedPax);
        }

        String routeKey = leg.origin() + "_" + leg.destination();
        var policy = kiponos.path("inventory", "overbook", "routes", routeKey);
        if (!policy.exists()) {
            policy = kiponos.path("inventory", "overbook", "default");
        }

        double ratio = policy.getFloat("overbook_ratio", 1.08);
        int maxStandby = policy.getInt("max_standby", 15);
        int effectiveCapacity = (int) (leg.physicalSeats() * ratio);
        int remaining = effectiveCapacity - confirmedPax;

        double bumpRisk = (double) confirmedPax / leg.physicalSeats();
        double threshold = policy.getFloat("bump_risk_threshold", 0.92);
        if (bumpRisk >= threshold) {
            remaining = Math.min(remaining, 0);
        }
        return new SeatAvailability(remaining, maxStandby);
    }
}
```

Every `getFloat()` is a **local memory read** — safe on search paths that evaluate thousands of legs per second during rebooking.

## Real scenarios

| Event | Frozen YAML reflex | Kiponos path |
|-------|-------------------|--------------|
| Hub weather meltdown | Emergency deploy to halt oversell | `inventory/irop/global_oversell_halt: true` |
| Single route crisis | Per-route code branch | `inventory/overbook/routes/JFK_BOS/overbook_ratio: 1.00` |
| Recovery day | Manual revert PR | Restore `overbook_ratio` from dashboard with audit |
| Crew repositioning surge | Standby list overflow | Lower `max_standby` on affected routes |
| Voucher budget breach | Finance escalates | Tune `bump_risk_threshold` tighter live |

## Performance — why availability search stays fast

- One WebSocket per inventory JVM — not one policy query per flight leg
- `getFloat("overbook_ratio")` is O(1) on the cached tree
- Delta updates — route-specific ratio change sends one patch
- Search threads never block on RM database round-trips for ops overrides
- `afterValueChanged` captures who tightened policy during IROP — compliance-friendly audit

## Compare to alternatives

| Approach | Halt oversell during IROP | Per-search read cost | Route-specific overrides |
|----------|---------------------------|----------------------|--------------------------|
| Seasonal `inventory-policy.yml` | PR + deploy | Zero (frozen) | Code branches |
| RM workstation batch | Hours | N/A until batch lands | RM tool only |
| Poll inventory DB | Possible | DB RTT × search volume | Schema migrations |
| **Kiponos SDK** | **Dashboard (seconds)** | **Memory read** | **Folder per route** |

## When not to use Kiponos for overbooking

| Case | Better approach |
|------|-----------------|
| Aircraft seat map / cabin config | Schedule system of record |
| DOT bump compensation rules | Compliance baseline in Git |
| Replacing RM with ML forecast model | Offline training pipeline |
| Physical aircraft swap | Operations control system |

## Getting started (15 minutes)

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['travel']['prod']['inventory']`.
2. Add `io.kiponos:sdk-boot-3` to your availability service.
3. Create `inventory/overbook/default` with `overbook_ratio`, `max_standby`, `bump_risk_threshold`.
4. Replace `@Value` inventory reads with `kiponos.path(...)`.
5. Game day: simulate IROP in staging, set `global_oversell_halt: true` live, re-query same leg — oversell stops **without pod restart**.

**Further reading:**

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for Java travel. Stop overselling while rebooking keeps flowing.*