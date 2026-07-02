---
title: "Control Accounting Month-End Close Rules at Runtime (Kiponos Java SDK)"
published: true
tags: java, accounting, fintech, realtime
description: Tune accrual thresholds, close calendars, and posting hold rules in Java ERP services during month-end crunch — Kiponos WebSocket deltas, zero-latency reads.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-accounting-month-end.md
main_image: https://files.catbox.moe/l631hl.jpg
---

Month-end close is controlled chaos. Controllers need to **freeze AP postings**, **extend accrual windows**, or **route exceptions to a manual queue** — at 11 PM on the last business day. Static ERP config means waking the on-call engineer to edit properties and restart Java posting services.

[Kiponos.io](https://kiponos.io) exposes close **control knobs** in a live tree: period status, module freeze flags, tolerance thresholds, and escalation timers — read locally by every posting JVM.

## Close gate in the posting path

```java
public PostingResult post(JournalEntry entry) {
    var close = kiponos.path("close", entry.ledgerId());
    if (close.getBool("posting_frozen")) {
        return PostingResult.rejected("period_frozen");
    }
    if (entry.amount().abs().compareTo(close.getBigDecimal("auto_post_max")) > 0) {
        return PostingResult.routeToWorkflow("amount_exceeds_auto_limit");
    }
    if (close.getBool("require_secondary_approval")) {
        return PostingResult.pendingApproval();
    }
    return ledger.post(entry);
}
```

Controllers flip `posting_frozen` in Kiponos — **not** via emergency deploy.

## Close control tree

```yaml
close/
  us-gaap/
    posting_frozen: false
    auto_post_max: 50000
    require_secondary_approval: false
    accrual_cutoff_hour: 18
    exception_queue_sla_hours: 4
  ifrs/
    posting_frozen: false
    auto_post_max: 25000
  global/
    month_end_mode: active
    notify_controller_on_hold: true
```

## Real-world scenarios

| Scenario | Live action |
|----------|-------------|
| Sub-ledger still reconciling | `posting_frozen: true` for US GAAP only |
| Large vendor invoice spike | Lower `auto_post_max` |
| Audit request | `require_secondary_approval: true` |
| Extended close window | Push `accrual_cutoff_hour` |

## Performance

Posting engines run high volume — config reads must be **in-memory**. Delta updates when controllers adjust tolerances mid-close.

## Getting started

1. [kiponos.io](https://kiponos.io) — profile `close/*` per ledger
2. Externalize freeze flags from ERP properties
3. Dry-run close: toggle `month_end_mode`; verify posting behavior

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for Java. Month-end controls when controllers need them — not when deploy windows allow.*