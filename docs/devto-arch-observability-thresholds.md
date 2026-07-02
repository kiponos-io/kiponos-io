---
title: "Live Observability Alert Thresholds — Stop Redeploying for Every SLO Tweak (Kiponos Python SDK)"
published: false
tags: python, devops, monitoring, architecture
description: Error-rate and latency alert thresholds trapped in Prometheus YAML or Python constants need live tuning. Kiponos feeds alert evaluators — calm false alarms during incidents without editing rules files.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-observability-thresholds.md
main_image: https://files.catbox.moe/87bzmo.jpg
---

Alert fatigue is a **configuration** problem wearing an operations mask. `error_rate > 0.01` pages during every deploy. During checkout it is too loose. On-call asks for `0.02` until the deploy finishes — and gets a Prometheus PR instead of relief.

[Kiponos.io](https://kiponos.io) feeds **custom SLO evaluator services** and **alert bots** with live thresholds. Prometheus stays your metrics store; Kiponos becomes the **tunable policy layer** that decides when metrics become pages.

## Two-layer observability

![Architecture diagram](https://files.catbox.moe/fewpug.png)

Evaluators run every N seconds — not per HTTP request — but still benefit from **local reads** and **mid-incident edits**.

## Evaluator code

```python
def should_page(metric: str, value: float, kiponos) -> bool:
    t = kiponos.path("alerts", metric)
    if kiponos.path("alerts", "global").get_bool("maintenance_mode"):
        return False
    if t.get_bool("silenced"):
        return False
    return value > t.get_float("critical_threshold")

def should_warn(metric: str, value: float, kiponos) -> bool:
    t = kiponos.path("alerts", metric)
    return value > t.get_float("warning_threshold")
```

## Alert tree

```yaml
alerts/
  payments_error_rate/
    warning_threshold: 0.005
    critical_threshold: 0.02
    silenced: false
  checkout_p99_ms/
    warning_threshold: 800
    critical_threshold: 1500
  global/
    maintenance_mode: false
deploy/
  active: true
  silence_all: false
```

## Deploy window playbook

1. Set `deploy/active: true` — evaluator doubles thresholds automatically (or `silence_all`)
2. Ship release
3. Clear deploy flags — thresholds snap back **without git revert**

## Closed loop with supervisor

Supervisor bot watches 24h variance, **writes** Kiponos when noise floor shifts — same architecture as [ML supervisor tuning](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-supervisor-ml-training.md):

```python
if rolling_std("payments_error_rate") < 0.001:
    kiponos.path("alerts", "payments_error_rate").set(
        "warning_threshold", 0.003
    )
```

## Real-world scenarios

| Scenario | Live tweak |
|----------|------------|
| Known deploy | `deploy/silence_all: true` |
| Noisy neighbor service | Raise one metric's `critical_threshold` |
| Real outage | Lower thresholds after deploy noise ends |
| Maintenance | `global/maintenance_mode: true` |

## Performance

Evaluators poll on interval — `get_float()` is **in-memory** per evaluation cycle.

## Compare

| Approach | On-call speed | Per-metric silencing |
|----------|---------------|----------------------|
| Prometheus YAML | PR + reload | Edit rules file |
| Hard-coded Python | Redeploy bot | Redeploy |
| **Kiponos tree** | **Dashboard** | **`silenced` flag** |

## Getting started

1. [kiponos.io](https://kiponos.io) — `alerts/*` folders per SLO
2. Point evaluator at Kiponos instead of constants
3. Drill: silence one metric during fake deploy

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — SLO thresholds you tune while the pager is still warm.*