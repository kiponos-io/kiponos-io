---
title: "Tune Retail Banking Loan Approval Thresholds in Real Time (Kiponos Java SDK)"
published: true
tags: java, banking, fintech, realtime
description: Change credit score cutoffs, DTI limits, and product-specific approval rules in Java origination services while applications keep flowing. Kiponos local reads, WebSocket deltas.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-banking-loan-approval.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-banking-loan.jpg
---

Loan origination is a pipeline of **policy thresholds**: minimum FICO, max debt-to-income, regional caps, promotional rate eligibility. Risk committee moves cutoffs after macro shifts — usually meaning **weeks** until IT deploys new `application.yml` across origination, underwriting, and pricing services.

[Kiponos.io](https://kiponos.io) centralizes approval policy in a live tree every Java service reads locally. Risk edits `min_fico` in the dashboard; the next application evaluation sees it — same pattern as [live fraud thresholds](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-fraud-payment-routing.md).

## Origination hot path

```java
public Decision evaluate(Application app) {
    var policy = kiponos.path("lending", "retail", app.productCode());
    if (app.fico() < policy.getInt("min_fico")) {
        return Decision.decline("below_fico_floor");
    }
    if (app.dti() > policy.getFloat("max_dti")) {
        return Decision.decline("dti_exceeded");
    }
    if (app.requestedAmount() > policy.getInt("max_loan_usd")) {
        return Decision.referManual("amount_cap");
    }
    return Decision.approve(pricing(app, policy));
}
```

`productCode()` selects `lending/retail/auto` vs `personal` subtrees — one hub, many products.

## Policy tree example

```yaml
lending/
  retail/
    auto/
      min_fico: 680
      max_dti: 0.45
      max_loan_usd: 75000
      promo_rate_enabled: true
    personal/
      min_fico: 640
      max_dti: 0.50
      max_loan_usd: 35000
    global/
      manual_review_band_fico: 620
      halt_new_apps: false
```

## Architecture

![Architecture diagram](https://files.catbox.moe/ma6sy2.png)

## Real-world scenarios

| Scenario | Live tweak |
|----------|------------|
| Rate hike environment | Raise `min_fico` across products |
| Auto promotion weekend | Enable `promo_rate_enabled` for auto only |
| Capacity constraint | Lower `max_loan_usd` |
| System stress | `halt_new_apps: true` — instant kill switch |

## Performance

Policy checks run **per application** — `getInt()` must stay local. WebSocket updates are async background work.

## Compare to alternatives

| Approach | Same-day policy change | Product-specific rules |
|----------|------------------------|------------------------|
| Hard-coded tables | No | Code branches |
| Rules engine DB | Possible | Another platform |
| **Kiponos tree** | **Dashboard** | **Folder per product** |

## Getting started

1. [kiponos.io](https://kiponos.io) TeamPro — `lending/retail/*`
2. Map credit policy PDF into config tree keys
3. Wire SDK in origination service
4. Shadow application: change `min_fico`; re-run decision without redeploy

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for Java. Loan policy that moves at the speed of risk committee — not release train.*