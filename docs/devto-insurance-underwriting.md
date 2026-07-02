---
title: "Live Insurance Underwriting Score Cutoffs (Kiponos Java SDK)"
published: true
tags: java, insurance, fintech, realtime
description: Adjust auto-decline scores, referral bands, and product-specific underwriting rules in Java policy engines at runtime. Kiponos zero-latency reads on every quote.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-insurance-underwriting.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-insurance.jpg
---

Underwriting models output a score; policy engines apply **cutoffs**: auto-bind below 40, refer 40–65, decline above 80. Actuarial shifts those bands after loss experience — usually trapped in spreadsheet → IT ticket → **deploy cycle** while agents still quote stale rules.

[Kiponos.io](https://kiponos.io) stores cutoffs and product rules in a live tree Java quote services read on every request.

## Quote decision path

```java
public QuoteDecision underwrite(QuoteRequest req, double modelScore) {
    var uw = kiponos.path("underwriting", req.productLine());
    if (modelScore >= uw.getInt("decline_above")) {
        return QuoteDecision.decline("score");
    }
    if (modelScore >= uw.getInt("refer_above")) {
        return QuoteDecision.refer("senior_uw");
    }
    if (req.coverageUsd() > uw.getInt("auto_bind_max_coverage")) {
        return QuoteDecision.refer("limit_exceeded");
    }
    return QuoteDecision.bind(computePremium(req, uw));
}
```

## Underwriting tree

```yaml
underwriting/
  auto/
    decline_above: 80
    refer_above: 55
    auto_bind_max_coverage: 500000
    surcharge_regions: FL,LA
  home/
    decline_above: 75
    refer_above: 50
    wind_hail_deductible_min: 5000
  global/
    pause_new_quotes: false
```

## Real-world scenarios

| Scenario | Live tweak |
|----------|------------|
| Catastrophe season | Raise `refer_above` in FL auto |
| Competitive push | Lower `decline_above` temporarily |
| Capacity full | `pause_new_quotes: true` |
| New rider launch | Add product folder without code deploy |

## Performance

Quote APIs are latency-sensitive — local `getInt()` only on hot path.

## Getting started

1. [kiponos.io](https://kiponos.io) — `underwriting/{product}/*`
2. Map actuarial tables to Kiponos keys
3. Live test: adjust `refer_above`; re-quote same risk profile

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for Java insurance. Underwriting cutoffs at actuarial speed.*