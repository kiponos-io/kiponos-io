---
title: "Change Retail Reorder Points and Safety Stock Live — No Java Restart (Kiponos SDK)"
published: false
tags: java, retail, inventory, supplychain
description: Tune reorder points, safety stock days, and supplier lead-time buffers in Java inventory services while purchase orders keep generating. Kiponos local reads on every stock evaluation.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-retail-inventory-reorder.md
main_image: https://litter.catbox.moe/bxi8vw.jpg
---

Black Friday hour 6. Your **#2 bestseller** hits zero on-hand in the East DC while the replenishment worker still uses `safety_stock_days: 14` from `inventory-policy.yml` — a number supply chain set in August when lead times averaged nine days.

The planner messages the war room:

> "Vendor just stretched lead time to **21 days** on that SKU family. We need reorder points **today** or we lose the whole weekend."

Engineering's answer is a config PR, integration tests, and a rolling restart of four inventory microservices. Purchase orders keep firing against stale thresholds. Stockouts spread to substitute SKUs.

**`safety_stock_days` is not architecture. It is how much buffer you want tonight given tonight's lead times.**

[Kiponos.io](https://kiponos.io) lets supply chain move reorder policy **while replenishment jobs keep running** — WebSocket deltas, in-memory reads on every stock evaluation.

## The problem: frozen policy on the replenishment hot path

```java
@ConfigurationProperties(prefix = "inventory.policy")
public class InventoryPolicyProperties {
    private int safetyStockDays = 14;
    private int reorderPointUnits = 120;
    private int maxOrderQuantity = 500;
}

@Service
public class ReorderEvaluator {
    private final InventoryPolicyProperties policy;

    public ReorderSignal evaluate(StockSnapshot stock) {
        int daysCover = stock.onHand() / stock.avgDailyDemand();
        if (daysCover < policy.getSafetyStockDays()) {
            return ReorderSignal.order(policy.getReorderPointUnits());
        }
        return ReorderSignal.hold();
    }
}
```

Problems on the hot path:

1. **Deploy to react** — while DCs bleed inventory hourly
2. **Per-SKU exceptions in code** — planner exceptions become engineering tickets
3. **DB config table poll** — adds latency when the evaluator runs across 200k SKUs nightly

| What teams say | What production does |
|----------------|---------------------|
| "Safety stock is an annual planning exercise" | Lead times and demand shift weekly |
| "We'll run a one-off SQL update" | Microservices still read YAML until restart |
| "WMS owns inventory policy" | Your Java orchestrator still computes reorder signals |
| "Just increase everything 20% for the holidays" | Overstock ties up capital after the event |

## The Aha: reorder thresholds are operational floats

Store replenishment policy under `inventory/reorder` in Kiponos. Each `evaluate()` reads `safety_stock_days`, `reorder_point_units`, and category overrides from the in-memory tree. When the planner raises East DC buffers, the **next** batch evaluation uses them — no service restart.

## What is Kiponos.io — for inventory orchestration

Kiponos connects your Spring Boot replenishment service to a live config tree. Profile `['retail']['prod']['inventory']` hydrates at startup. Dashboard edits are **WebSocket deltas** — one key changes, one node patches in memory. `kiponos.path("inventory", "reorder", sku.category()).getInt("safety_stock_days")` is a **local read** — no JDBC, no Redis RTT on every SKU in the nightly batch.

Separate **wiring** (team credentials in `application.yml`) from **operational policy** (safety stock, reorder points, supplier buffers) that planners move during supply shocks.

## Architecture

![Architecture diagram](https://files.catbox.moe/2gxy5w.png)

## Example config tree

```yaml
inventory/
  reorder/
    default/
      safety_stock_days: 14
      reorder_point_units: 120
      max_order_quantity: 500
      lead_time_buffer_days: 3
    electronics/
      safety_stock_days: 21
      reorder_point_units: 80
      priority_vendor: acme-components
    seasonal/
      safety_stock_days: 28
      holiday_mode: true
  global/
    halt_auto_reorder: false
    min_days_cover_floor: 5
  suppliers/
    acme/
      lead_time_days_override: 21
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
public class KiponosReorderEvaluator {

    private final Kiponos kiponos;

    public KiponosReorderEvaluator(Kiponos kiponos) {
        this.kiponos = kiponos;
        kiponos.afterValueChanged(change -> {
            if (change.path().startsWith("inventory/global/halt")) {
                log.warn("Auto-reorder policy changed: {} → {}", change.path(), change.newValue());
            }
        });
    }

    public ReorderSignal evaluate(StockSnapshot stock) {
        if (kiponos.path("inventory", "global").getBool("halt_auto_reorder", false)) {
            return ReorderSignal.hold("global_halt");
        }

        var policy = kiponos.path("inventory", "reorder", stock.category());
        int safetyDays = policy.getInt("safety_stock_days", 14);
        int reorderUnits = policy.getInt("reorder_point_units", 120);
        int maxOrder = policy.getInt("max_order_quantity", 500);
        int leadBuffer = kiponos.path("inventory", "suppliers", stock.vendorId())
                .getInt("lead_time_days_override", policy.getInt("lead_time_buffer_days", 3));

        int daysCover = stock.onHand() / Math.max(1, stock.avgDailyDemand());
        int effectiveSafety = safetyDays + leadBuffer;

        if (daysCover < effectiveSafety) {
            int qty = Math.min(reorderUnits, maxOrder);
            return ReorderSignal.order(qty, "below_safety_cover");
        }
        return ReorderSignal.hold();
    }
}
```

Every `getInt()` is a **local memory read** — safe when the nightly job evaluates hundreds of thousands of SKUs.

## Real scenarios

| Event | Frozen YAML reflex | Kiponos path |
|-------|-------------------|--------------|
| Vendor lead time stretch | PR across inventory services | `inventory/suppliers/acme/lead_time_days_override: 21` |
| Holiday demand spike | Pre-provisioned branches | `inventory/reorder/seasonal/safety_stock_days: 28` |
| Overstock after event | Manual PO holds + deploy to lower buffers | Reduce `reorder_point_units` live |
| Quality hold on category | Emergency stop | `inventory/global/halt_auto_reorder: true` |
| East DC allocation crisis | Per-DC code fork | Category subtree `electronics` without redeploy |

## Performance — why replenishment batches stay fast

- One WebSocket per inventory JVM — not one policy query per SKU
- `getInt("safety_stock_days")` is O(1) on the cached tree
- Delta updates — changing safety stock from 14 → 21 sends one patch
- Evaluator threads never block on config network I/O
- `afterValueChanged` for audit when planners move buffers during a supply shock

## Compare to alternatives

| Approach | Same-day reorder policy change | Per-SKU evaluation read cost |
|----------|-------------------------------|------------------------------|
| `inventory-policy.yml` | PR + deploy | Zero (frozen) |
| ERP spreadsheet → manual | Human bottleneck | N/A |
| Poll inventory DB config | Possible | DB RTT × SKU count |
| **Kiponos SDK** | **Dashboard (seconds)** | **Memory read** |

## When not to use Kiponos for inventory

| Case | Better approach |
|------|-----------------|
| Bill of materials / SKU master | PIM / ERP source of truth |
| Physical cycle-count schedules | WMS workflow |
| Replacing evaluator with ML forecast | Model training pipeline |
| Supplier contract legal minimums | Contract system of record |

## Getting started (15 minutes)

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['retail']['prod']['inventory']`.
2. Add `io.kiponos:sdk-boot-3` to your replenishment service.
3. Create `inventory/reorder/default` with `safety_stock_days`, `reorder_point_units`, `max_order_quantity`.
4. Replace `@ConfigurationProperties` policy reads with `kiponos.path(...)`.
5. Game day: simulate stock drop in staging, raise `safety_stock_days` live, re-run evaluator — reorder signal changes **without pod restart**.

**Further reading:**

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for Java retail. Reorder before the shelf goes empty.*