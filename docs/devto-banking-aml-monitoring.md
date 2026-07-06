---
title: "Update AML Monitoring Rules Without Restarting Your Java Banking Stack (Kiponos SDK)"
published: true
tags: java, banking, security, realtime
description: Live AML velocity limits, CTR thresholds, and SAR triggers in Java transaction monitoring. Kiponos WebSocket deltas with zero-latency reads on every screened event.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-banking-aml-monitoring.md
main_image: https://files.catbox.moe/574r3o.jpg
---

AML typologies evolve faster than quarterly releases. A new smurfing pattern appears Tuesday; compliance needs **tighter velocity caps**, a **lower CTR threshold**, and **enhanced country monitoring** — while your Java event fabric processes millions of transactions per hour.

[Kiponos.io](https://kiponos.io) delivers AML parameters to every screening JVM over WebSocket. Analysts edit the dashboard; SDKs merge **delta patches** into in-memory trees; the next screened event uses updated rules — **no rolling restart**, no config promotion across `dev → uat → prod`.

## The screening hot path

```java
public AmlDecision screen(Transaction txn, CustomerProfile customer) {
    var aml = kiponos.path("aml", "retail");
    if (txn.amountUsd() >= aml.path("thresholds").getInt("ctr_threshold_usd")) {
        return AmlDecision.fileCtr(txn);
    }
    if (hourlyVelocity(customer.id()) > aml.path("velocity").getInt("hourly_txn_cap")) {
        return AmlDecision.alert("velocity_exceeded");
    }
    if (matchesWatchlist(txn, aml.path("lists"))) {
        return AmlDecision.block("watchlist_hit");
    }
    return AmlDecision.pass();
}
```

Every `getInt()` is **local** — mandatory when screening runs at bank-scale QPS.

## Why static AML config fails

| Static approach | Operational pain |
|-----------------|------------------|
| Rules in YAML | Emergency deploy during fraud spike |
| Rules DB polled per txn | Latency + DB load on hot path |
| Separate rules engine | Another system to sync and audit |

Compliance needs **human-in-the-loop** changes with **audit trail** — Kiponos dashboard edits plus `afterValueChanged` listeners for SIEM.

## AML config tree

```yaml
aml/
  retail/
    thresholds/
      ctr_threshold_usd: 10000
      sar_auto_file_score: 92
      review_score: 75
    velocity/
      hourly_txn_cap: 12
      daily_amount_usd: 25000
    lists/
      high_risk_countries: RU,NG,IR
      enhanced_monitoring_mccs: 7995,6012
    modes/
      enhanced_due_diligence: false
      pause_wire_transfers: false
```

## Architecture

![Architecture diagram](https://files.catbox.moe/szfze6.png)

All channels read the **same** AML tree — consistent policy without copying rules into three repos.

## Audit and governance

```java
kiponos.afterValueChanged(change ->
    siem.emit("aml_config_change", Map.of(
        "path", change.path(),
        "old", change.oldValue(),
        "new", change.newValue(),
        "actor", change.metadata().get("editor")
    ))
);
```

Pair dashboard ACLs with change listeners for **regulatory evidence**.

## Real-world scenarios

| Scenario | Live Kiponos action |
|----------|---------------------|
| New typology alert | Lower `hourly_txn_cap` |
| Holiday wire volume | Raise `ctr_threshold_usd` temporarily |
| Sanctions update | Edit `high_risk_countries` |
| Partner bank outage | `pause_wire_transfers: true` |

## Performance

- **One WebSocket** per screening service JVM
- **O(1) reads** per rule evaluation
- **Delta updates** — one threshold change does not reload entire rule pack

## Compare to alternatives

| Approach | Mid-day rule change | Consistent across channels |
|----------|---------------------|----------------------------|
| Per-service YAML | Restart fleet | Drift |
| Central rules DB | Poll latency | Possible |
| **Kiponos AML tree** | **Dashboard** | **Single hub** |

## Getting started

1. [Free TeamPro at kiponos.io](https://kiponos.io) — profile `aml/retail/*`
2. Model current rule pack as Kiponos folders
3. Replace constants in screening services with `kiponos.path("aml", ...).get*()`
4. Tabletop exercise: flip `enhanced_due_diligence`; confirm all JVMs log change

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for Java banking. AML rules that keep pace with typologies — without restart windows.*