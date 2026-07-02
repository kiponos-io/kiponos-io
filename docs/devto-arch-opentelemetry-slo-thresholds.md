---
title: "OpenTelemetry SLO Burn-Rate Windows You Can Retune Live — Without Redeploying Alert Rules (Kiponos Python SDK)"
published: false
tags: python, observability, slo, devops
description: Error-budget burn-rate windows and paging thresholds trapped in Prometheus YAML or OTel collector config need live tuning during incidents. Kiponos feeds SLO evaluators — change burn windows without editing alert rules.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-opentelemetry-slo-thresholds.md
main_image: https://files.catbox.moe/87bzmo.jpg
cover_image: /home/moshe/work/kiponos-io/docs/devto-cover-arch-opentelemetry-slo.jpg
---

Tuesday 03:14. Checkout SLO is bleeding error budget — but the **fast-burn** alert never fired because `burn_rate_window_minutes: 5` was copied from a template when you adopted OpenTelemetry six months ago. The **slow-burn** window at 60 minutes pages every deploy noise spike.

SRE lead in the bridge:

> "Burn-rate windows are **SLO architecture**. They ship with the observability stack."

They ship with the observability stack the same way `failureRateThreshold` shipped with Resilience4j — until production proves otherwise. During a real outage, **how long you look back** and **how fast you page** are operational knobs, not semver artifacts.

[Kiponos.io](https://kiponos.io) sits between your **OTel metrics pipeline** (unchanged) and your **SLO evaluator** (live policy). Prometheus or your metrics backend stays the source of truth for counters and histograms. Kiponos holds the **burn-rate windows, multipliers, and silence flags** your evaluator reads every scrape cycle — local `get()`, WebSocket deltas, no redeploy.

## The problem: frozen burn-rate policy

Multi-window, multi-burn-rate alerting (Google SRE book pattern) typically hard-codes windows in YAML or Python:

```python
FAST_BURN_WINDOW = timedelta(minutes=5)
SLOW_BURN_WINDOW = timedelta(minutes=60)
FAST_BURN_MULTIPLIER = 14.4
SLOW_BURN_MULTIPLIER = 6.0
ERROR_BUDGET_FRACTION = 0.001  # 0.1% monthly budget → hourly rate
```

Your evaluator pulls OTel-exported `http_server_request_duration` and `http_server_request_errors`, computes burn rate over those windows, and pages when thresholds breach. Works until:

- Deploy noise triggers **slow-burn** for an hour while the team is already in the bridge
- A real incident needs a **tighter fast window** (2 minutes) but YAML change waits on the observability repo CODEOWNERS
- Black Friday needs **relaxed windows** so marketing traffic does not exhaust budget on expected 503s

The hot path is not per-span — evaluators run every 30–60 seconds. Still, **policy constants** should not require a collector reload or bot redeploy mid-incident.

## What teams believe vs production

| Belief | Production reality |
|--------|-------------------|
| "Burn windows belong in Prometheus rules" | Rule reload still needs review; bad timing during outage |
| "OTel Collector config is infrastructure" | Collector restart mid-incident is scary |
| "We silence in PagerDuty" | PD silences do not fix wrong math — next deploy re-alerts |
| "Feature flags for SLOs" | Boolean-centric; awkward for `window_minutes` floats |

## The Aha

**Burn-rate windows and multipliers are operational parameters** — tune them while the evaluator keeps running. Point your SLO bot at a Kiponos tree; ops widens `slow_burn_window_minutes` to 120 during deploy, tightens `fast_burn_window_minutes` to 2 when budget is critical.

## How Kiponos fits OpenTelemetry SLO evaluation

OpenTelemetry gives you **portable metrics** — counters, histograms, exemplars — exported to Prometheus, Grafana Mimir, or Datadog. Kiponos does not replace OTel instrumentation or the collector. It replaces **hard-coded policy** in the service that turns metrics into pages.

1. **Instrument** with OTel SDK (unchanged) — `http.server.duration`, error status codes
2. **Export** via OTLP to your backend (unchanged)
3. **Evaluate** in a small Python service that reads burn policy from Kiponos every cycle
4. **Ops edits** `slo/checkout/fast_burn_window_minutes` in dashboard → WebSocket delta → next evaluation uses new window

## Architecture

![Architecture diagram](https://files.catbox.moe/wnwynk.png)

## Config tree example

```yaml
slo/
  checkout/
    error_budget_fraction: 0.001
    fast_burn_window_minutes: 5
    slow_burn_window_minutes: 60
    fast_burn_multiplier: 14.4
    slow_burn_multiplier: 6.0
    min_requests_in_window: 100
    silenced: false
  payments/
    error_budget_fraction: 0.0005
    fast_burn_window_minutes: 3
    slow_burn_window_minutes: 45
    fast_burn_multiplier: 18.0
    slow_burn_multiplier: 8.0
    silenced: false
  global/
    deploy_silence_all: false
    maintenance_mode: false
    default_query_step_seconds: 30
```

## Integration: OTel metrics + live Kiponos policy

```python
from datetime import timedelta
from kiponos import Kiponos

kiponos = Kiponos.create_for_current_team()


def burn_rate(error_count: float, total_count: float, budget_fraction: float,
              window_minutes: int) -> float:
    if total_count == 0:
        return 0.0
    error_rate = error_count / total_count
    hourly_budget = budget_fraction / (30 * 24)  # monthly → hourly
    window_hours = window_minutes / 60.0
    return error_rate / (hourly_budget * window_hours)


def evaluate_slo(service: str, errors: float, total: float) -> str | None:
    cfg = kiponos.path("slo", service)
    global_cfg = kiponos.path("slo", "global")

    if global_cfg.get_bool("maintenance_mode") or global_cfg.get_bool("deploy_silence_all"):
        return None
    if cfg.get_bool("silenced"):
        return None
    if total < cfg.get_int("min_requests_in_window", 100):
        return None

    budget = cfg.get_float("error_budget_fraction", 0.001)
    fast_win = cfg.get_int("fast_burn_window_minutes", 5)
    slow_win = cfg.get_int("slow_burn_window_minutes", 60)
    fast_mult = cfg.get_float("fast_burn_multiplier", 14.4)
    slow_mult = cfg.get_float("slow_burn_multiplier", 6.0)

    fast_br = burn_rate(errors, total, budget, fast_win)
    slow_br = burn_rate(errors, total, budget, slow_win)

    if fast_br > fast_mult:
        return f"FAST_BURN {service} rate={fast_br:.1f}x"
    if slow_br > slow_mult:
        return f"SLOW_BURN {service} rate={slow_br:.1f}x"
    return None
```

Query OTel-backed metrics (Prometheus API example):

```python
def query_range(prom, query: str, window_minutes: int, step: int) -> tuple[float, float]:
    # Returns (error_count, total_count) for the window — implementation specific
    ...
```

Wire evaluator loop:

```python
while True:
    step = kiponos.path("slo", "global").get_int("default_query_step_seconds", 30)
    for service in ("checkout", "payments"):
        cfg = kiponos.path("slo", service)
        fast_m = cfg.get_int("fast_burn_window_minutes", 5)
        errors, total = query_range(prom, f'sum(rate(http_errors{{service="{service}"}}[5m]))', fast_m, step)
        alert = evaluate_slo(service, errors, total)
        if alert:
            page(alert)
    sleep(step)
```

`get_int()` / `get_float()` are **in-process reads** — no hub round-trip per evaluation tick.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| Deploy starts | Slow-burn pages on expected 502s | `deploy_silence_all: true` for 20 min |
| Budget nearly exhausted | Wait for YAML PR to tighten fast window | `fast_burn_window_minutes: 2` live |
| Marketing load test | False fast-burn on low traffic denominator | Raise `min_requests_in_window` live |
| Incident winding down | Manually revert rule file next day | Restore windows from hub snapshot |

## Performance

- Evaluator reads policy from memory each cycle — **nanoseconds**, not milliseconds
- OTel export path unchanged — no extra span attributes for policy
- WebSocket deltas are small — one key change does not reload full tree
- Query step driven by `default_query_step_seconds` — tune load on metrics backend live

## Compare to alternatives

| Approach | Mid-incident window tweak | Per-service silencing |
|----------|---------------------------|------------------------|
| Prometheus recording rules | Edit + reload rules | Rule file per service |
| OTel Collector processor config | Collector rollout | Restart collector |
| Hard-coded Python constants | Redeploy evaluator | Redeploy |
| **Kiponos tree** | **Dashboard** | **`silenced` flag** |

## When not to use Kiponos

| Case | Better tool |
|------|-------------|
| Defining SLI formulas (histogram quantiles) | OTel views + recording rules in Git |
| Long-term SLO dashboard charts | Grafana / Datadog as-is |
| PagerDuty routing schedules | PD escalation policies |
| Storing raw metrics | Prometheus / Mimir — not config hub |

## Getting started (15 minutes)

1. [Create TeamPro at kiponos.io](https://kiponos.io) — profile `['sre']['checkout']['prod']['slo']`
2. Keep OTel instrumentation — add nothing to the request path
3. Move burn-window constants from Python module into `slo/*` tree keys above
4. Run evaluator as CronJob or sidecar; replace `FAST_BURN_WINDOW` with `kiponos.path(...).get_int(...)`
5. Game day: trigger fake burn, then widen `slow_burn_window_minutes` live and watch alerts calm

## Further reading

- [Developer Quickstart](https://dev.to/kiponos/kiponosio-developer-quickstart-java-python-and-your-first-live-config-change-3kjo)
- [Live observability thresholds (related)](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-observability-thresholds.md)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — OpenTelemetry measures the fire. Live SLO policy decides when to call the brigade.*