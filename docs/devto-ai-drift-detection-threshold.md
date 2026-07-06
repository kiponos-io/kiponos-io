---
title: "PSI_ALERT=0.25 and KS_PVALUE=0.01 Were Frozen in monitor.py — Drift Storm False-Alarmed Retrain (Python + Kiponos)"
published: false
tags: python, ai, mlops, monitoring
description: PSI and KS drift thresholds feel like statistical constants checked into monitoring code. During distribution shifts they are operational alert policy — Kiponos feeds live cutoffs with zero-latency reads per batch.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-ai-drift-detection-threshold.md
main_image: https://files.catbox.moe/n1iaiq.jpg
---

Wednesday 1:15 PM. Your fraud scoring service runs nightly drift checks — PSI on `transaction_amount`, KS on `merchant_category`, population stability on geolocation buckets. `monitor.py` exports `PSI_ALERT = 0.25`, `KS_PVALUE_CUTOFF = 0.01`, and `RETRAIN_COOLDOWN_HOURS = 24` because the data science lead picked "textbook" cutoffs during model v3 launch.

Black Friday week arrives. Legitimate spend shifts — higher ticket sizes, new merchant mix, expected seasonality. PSI on `transaction_amount` reads **0.31** — above threshold. The auto-retrain pipeline fires, queues a full GPU job, and pages ML on-call every four hours while **precision on live traffic is still 0.94**.

The head of risk says:

> "Raise PSI alert to **0.40** and throttle retrain to **once per 72 hours** until seasonality ends. Do not redeploy the monitor workers."

But thresholds are floats in a module imported at scheduler boot. Changing them means recycling Airflow-sidecar workers and losing in-flight batch checkpoints. The model is fine. **Drift alert policy** is crying wolf *right now*.

Here is the Aha:

**PSI and KS cutoffs behave like immutable statistical law, but they are operational alert policy for this distribution hour.**

You can change `psi_alert`, `ks_pvalue_cutoff`, and `retrain_cooldown_hours` **while monitor workers keep scoring batches** — no redeploy, no restart, no poll per feature column. The next `evaluate_drift()` already reads new thresholds from memory. That is [Kiponos.io](https://kiponos.io).

## The problem — frozen drift thresholds on the batch hot path

```python
# monitor.py
PSI_ALERT = 0.25
KS_PVALUE_CUTOFF = 0.01
RETRAIN_COOLDOWN_HOURS = 24
RETRAIN_MIN_SAMPLES = 50_000
```

Every nightly batch inherits those decisions:

```python
def evaluate_drift(reference, production, feature: str) -> DriftResult:
    psi = population_stability_index(reference[feature], production[feature])
    ks_stat, ks_p = ks_2samp(reference[feature], production[feature])

    alert = psi >= PSI_ALERT or ks_p < KS_PVALUE_CUTOFF
    return DriftResult(feature=feature, psi=psi, ks_p=ks_p, alert=alert)
```

Seasonal shift needs looser PSI and longer retrain cooldown **now**. ML cannot patch running monitors. Teams **know** false-positive retrain is expensive — they do not know cutoffs can move **without recycling scheduler workers**.

| What teams believe | What production does |
|------------------|---------------------|
| "PSI > 0.25 is industry standard" | Seasonality violates standard without being model decay |
| "Thresholds belong in DS sign-off docs" | Ops needs looser alerts during known events |
| "Retrain cooldown protects GPU budget" | Cooldown itself must move during sale week |
| "monitor.py constants are reproducible science" | Reproducibility ≠ inability to tune live |

## The Aha — live drift policy while batches run

```yaml
drift/
  thresholds/
    psi_alert: 0.25
    ks_pvalue_cutoff: 0.01
    min_samples: 50000
  retrain/
    cooldown_hours: 24
    max_retrains_per_week: 3
    auto_retrain_enabled: true
  seasonality/
    sale_week_mode: false
    sale_week_psi_alert: 0.40
    sale_week_cooldown_hours: 72
    sale_week_auto_retrain: false
```

Risk ops enables `seasonality/sale_week_mode`. Dashboard sets `sale_week_psi_alert: 0.40`. **Next batch evaluation** uses the relaxed cutoff — local `get_float()`, zero network.

## What is Kiponos.io — for drift alert governance

Kiponos holds drift policy in memory for profile `['fraud-ml']['prod']['drift']`. `kiponos.path("drift", "thresholds").get_float("psi_alert")` is a **local read** inside `evaluate_drift()` — no HTTP per feature across hundreds of columns.

`after_value_changed` can flip `auto_retrain_enabled` off within seconds when risk declares manual review mode — the **next** alert evaluation respects the new boolean without redeploy.

Git keeps **which features you monitor**. The hub keeps **when PSI constitutes an emergency this week**.

## Architecture

![Architecture diagram](https://files.catbox.moe/9gdmhn.png)

1. **Connect once** at worker boot.
2. **Snapshot** for `['fraud-ml']['prod']['drift']`.
3. **Delta** on threshold edit.
4. **Async merge** on WebSocket thread.
5. **Local reads** per drift evaluation.

## Config tree

```yaml
drift/
  thresholds/
    psi_alert: 0.25
    ks_pvalue_cutoff: 0.01
    min_samples: 50000
    per_feature_overrides: transaction_amount:0.35,merchant_category:0.30
  retrain/
    cooldown_hours: 24
    max_retrains_per_week: 3
    auto_retrain_enabled: true
    require_manual_approval: false
  seasonality/
    sale_week_mode: false
    sale_week_psi_alert: 0.40
    sale_week_cooldown_hours: 72
    sale_week_auto_retrain: false
  paging/
    page_on_alert: true
    suppress_duplicate_hours: 6
```

## Integration — Kiponos-backed drift evaluation

```python
import logging
import os
import time
from dataclasses import dataclass

from kiponos import Kiponos
from scipy.stats import ks_2samp

log = logging.getLogger(__name__)

os.environ.setdefault("KIPONOS_PROFILE", "['fraud-ml']['prod']['drift']")
kiponos = Kiponos.create_for_current_team()

_last_retrain_ts: float | None = None


@dataclass(frozen=True)
class DriftPolicy:
    psi_alert: float
    ks_pvalue_cutoff: float
    min_samples: int
    cooldown_hours: int
    auto_retrain: bool


def population_stability_index(reference, production) -> float:
    # Simplified PSI for illustration
    import numpy as np
    ref_pct = np.histogram(reference, bins=10)[0] / max(len(reference), 1)
    prod_pct = np.histogram(production, bins=10)[0] / max(len(production), 1)
    eps = 1e-6
    return float(np.sum((prod_pct - ref_pct) * np.log((prod_pct + eps) / (ref_pct + eps))))


def _parse_override(feature: str, default: float) -> float:
    overrides = kiponos.path("drift", "thresholds").get("per_feature_overrides", "")
    for pair in overrides.split(","):
        if ":" not in pair:
            continue
        name, val = pair.split(":", 1)
        if name.strip() == feature:
            return float(val.strip())
    return default


def _load_policy(feature: str) -> DriftPolicy:
    season = kiponos.path("drift", "seasonality")
    thresh = kiponos.path("drift", "thresholds")
    retrain = kiponos.path("drift", "retrain")

    if season.get_bool("sale_week_mode", False):
        psi = season.get_float("sale_week_psi_alert", 0.40)
        cooldown = season.get_int("sale_week_cooldown_hours", 72)
        auto = season.get_bool("sale_week_auto_retrain", False)
    else:
        psi = _parse_override(feature, thresh.get_float("psi_alert", 0.25))
        cooldown = retrain.get_int("cooldown_hours", 24)
        auto = retrain.get_bool("auto_retrain_enabled", True)

    return DriftPolicy(
        psi_alert=psi,
        ks_pvalue_cutoff=thresh.get_float("ks_pvalue_cutoff", 0.01),
        min_samples=thresh.get_int("min_samples", 50_000),
        cooldown_hours=cooldown,
        auto_retrain=auto,
    )


def _on_policy_change(change) -> None:
    if not str(change.path).startswith("drift/"):
        return
    log.warning("Drift policy changed: %s — next batch uses new thresholds", change.path)


kiponos.after_value_changed(_on_policy_change)


@dataclass
class DriftResult:
    feature: str
    psi: float
    ks_p: float
    alert: bool
    retrain_allowed: bool


def _cooldown_ok(policy: DriftPolicy) -> bool:
    global _last_retrain_ts
    if _last_retrain_ts is None:
        return True
    elapsed_h = (time.time() - _last_retrain_ts) / 3600
    return elapsed_h >= policy.cooldown_hours


def evaluate_drift(reference, production, feature: str) -> DriftResult:
    policy = _load_policy(feature)
    if len(production) < policy.min_samples:
        return DriftResult(feature, 0.0, 1.0, alert=False, retrain_allowed=False)

    psi = population_stability_index(reference[feature], production[feature])
    _, ks_p = ks_2samp(reference[feature], production[feature])
    alert = psi >= policy.psi_alert or ks_p < policy.ks_pvalue_cutoff

    retrain_allowed = (
        alert
        and policy.auto_retrain
        and _cooldown_ok(policy)
        and not kiponos.path("drift", "retrain").get_bool("require_manual_approval", False)
    )

    if alert and kiponos.path("drift", "paging").get_bool("page_on_alert", True):
        log.warning("Drift alert feature=%s psi=%.3f ks_p=%.4f", feature, psi, ks_p)

    return DriftResult(feature, psi, ks_p, alert, retrain_allowed)


def maybe_trigger_retrain(result: DriftResult, pipeline) -> None:
    global _last_retrain_ts
    if not result.retrain_allowed:
        return
    pipeline.enqueue(feature=result.feature)
    _last_retrain_ts = time.time()
    log.warning("Retrain enqueued for %s (cooldown=%sh)", result.feature, _load_policy(result.feature).cooldown_hours)
```

Black Friday? Enable `sale_week_mode`. **Next batch** needs PSI 0.40 before alerting. Auto-retrain pauses until mode disables — no monitor worker restart.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| Seasonal PSI spike | False-positive retrain queue | `sale_week_psi_alert: 0.40` live |
| True model decay post-incident | Wait for deploy to tighten alerts | Lower `psi_alert` to 0.15 instantly |
| GPU budget exhaustion | Static 24h cooldown insufficient | `cooldown_hours: 72` dashboard |
| Per-feature merchant drift | One global threshold misfires | `per_feature_overrides` delta |
| Manual review window | Disable auto-retrain in code | `require_manual_approval: true` |

Pair with [realtime ML training](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-realtime-ml-training.md) when drift triggers live hyperparameter adjustments instead of full retrain.

## Compare to alternatives

| Approach | Loosen PSI during sale week | Per-batch overhead |
|----------|----------------------------|-------------------|
| `monitor.py` constants | PR + worker recycle | Zero (frozen) |
| Notebook parameter change | Not production path | N/A |
| Poll S3 for thresholds | Ops-fast | RTT × features |
| MLflow experiment tags | Audit trail | Not sub-second live ops |
| **Kiponos SDK** | **Dashboard delta (seconds)** | **Memory read** |

## Performance — why drift monitors care

- **Threshold reads are O(1)** — safe across hundreds of features per batch
- **One WebSocket per worker** — not HTTP to experiment tracking per column
- **`_load_policy(feature)` resolves overrides once** — no nested remote calls
- **`after_value_changed` logs policy transitions** — audit trail without redeploy forensics
- **Cooldown hours move live** — `_cooldown_ok` respects new integer on next batch

## When not to use Kiponos for drift policy

| Case | Better approach |
|------|-----------------|
| Changing PSI binning algorithm | Code review + deployment |
| Reference dataset snapshot selection | Data pipeline versioning |
| Feature engineering logic | Git + deploy |
| Model artifact promotion gates | CI/CD + MLflow registry |
| Regulatory frozen thresholds (mandated 0.25) | Compliance constant in code |

## Getting started (15 minutes)

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['fraud-ml']['prod']['drift']`.
2. Move `PSI_ALERT`, `KS_PVALUE_CUTOFF`, and `RETRAIN_COOLDOWN_HOURS` into the hub.
3. Wire `_load_policy(feature)` at the top of `evaluate_drift()`.
4. Add `seasonality/sale_week_mode` keys for event drills.
5. Rehearsal: enable sale week mode in staging, confirm fewer false retrains **without worker restart**.
6. Document boundary: Git declares drift math; hub declares **operational alert cutoffs**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — PSI cutoffs are when you panic-retrain, not monitor.py forever.*