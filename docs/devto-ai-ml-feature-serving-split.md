---
title: "One Feature Tree for Training and Serving Was the Lie — Split Offline vs Online Config (Architecture)"
published: false
tags: architecture, machinelearning, java, python
description: Training pipelines and inference services need different config trees, change cadence, and ownership. An architecture guide for splitting offline feature engineering from online serving with Kiponos Java and Python SDKs.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-ai-ml-feature-serving-split.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-ai-ml-feature-serving-split.jpg
---

Model launch week. Training reports **0.91 AUC** offline. Production serving shows **0.74** by Thursday. The postmortem finds the same `features.yaml` mounted in both the Spark batch job and the Java inference service — but training used a **seven-day aggregation window** baked into the offline job code while serving read `aggregation_days: 1` from a shared config file someone "fixed" during a latency sprint without retraining.

The ML platform lead closes the doc:

> "We need **one source of truth** for features."

They are half right. You need **one governance model** — not **one tree**. Training and serving are different **runtime classes**: different change velocity, different blast radius, different owners. Stuffing both into a single YAML file (or a single Kiponos profile without boundaries) recreates the skew incident with better tooling.

[Kiponos.io](https://kiponos.io) helps when you **split config trees deliberately** — offline training features in a Python profile, online serving features in a Java profile — and still change operational knobs **live** without restarts on either side.

## The problem: shared feature config across unlike runtimes

Most teams start with one file:

```yaml
# features.yaml — "shared" between batch and serving
user_engagement/
  aggregation_days: 7
  min_events: 3
  decay_factor: 0.95
pricing/
  include_promo: true
  currency: USD
```

Offline training (Python) reads it at job start. Online serving (Java) reads it per request — or worse, cached at pod start. Then:

1. **Serving lowers `aggregation_days` to 1** for latency — nobody retrains
2. **Training adds `experimental_signal`** — serving never sees it until deploy
3. **Ops toggles `include_promo` live** — training data still reflects old policy

The hot path in serving cannot poll S3. The training loop cannot restart for every experiment. You need **two trees**, same hub, different profile paths — both with **local SDK reads**.

| What teams say | What production does |
|----------------|---------------------|
| "DRY — one features.yaml" | Training and serving diverge silently |
| "Feature store solves this" | Store holds values; policy for windows and fallbacks still needs runtime config |
| "We'll version the file" | Versioning without runtime split does not stop serving from reading wrong window |

## The Aha: offline and online trees are siblings, not duplicates

**Training feature policy** (`aggregation_days`, sampling rates, experimental columns) is **offline operational** — changes during experiments, read every batch in Python.

**Serving feature policy** (fallback values, cache TTL, imputation rules, online-only caps) is **online operational** — changes during incidents, read every request in Java.

Both belong in Kiponos. They do **not** belong in the same profile folder.

```yaml
# Profile A: ['ml']['fraud']['offline']['features']  — Python trainer
# Profile B: ['ml']['fraud']['online']['features']   — Java inference service
```

Link them with **explicit contract keys** (`schema_version`, `feature_set_id`) in Git — not by copying aggregation knobs between trees.

## What Kiponos.io is — for split ML feature trees

[Kiponos.io](https://kiponos.io) is a real-time config hub with **Java** (Spring Boot 2/3) and **Python** SDKs. Each runtime connects once over WebSocket, hydrates **its** profile subtree, and serves **local** `get*()` on the hot path.

- **Python training workers** read `offline/features` every batch — tune `sample_rate`, `aggregation_days` during experiments ([realtime ML training](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-realtime-ml-training.md)).
- **Java inference services** read `online/features` every request — tune `default_missing`, `cache_ttl_sec` during incidents without restart.

Updates are **async deltas**. No redeploy. No cross-network read on the hot path. Profile path pattern: `['app']['release']['env']['config']` per service.

## Architecture

![Architecture diagram](https://litter.catbox.moe/xzcsj2.png)

**Bootstrap in Git, operate in hub.** Git declares `feature_set_id` and contract version. Day-2 tuning happens in the correct tree with ACL — offline keys never accidentally patch serving fallbacks.

## Config trees (two profiles)

**Offline — Python training** (`['ml']['fraud']['offline']['features']`):

```yaml
offline/
  engagement/
    aggregation_days: 7
    min_events: 3
    decay_factor: 0.95
    sample_rate: 1.0
  experiment/
    include_synthetic_negatives: false
    max_rows_per_epoch: 500000
  contract/
    feature_set_id: fraud-v4
    schema_version: 12
```

**Online — Java serving** (`['ml']['fraud']['online']['features']`):

```yaml
online/
  engagement/
    aggregation_days: 7
    default_missing: 0.0
    cache_ttl_sec: 300
    max_staleness_sec: 900
  imputation/
    use_median_fallback: true
    clip_outliers: true
    outlier_cap_sigma: 4.0
  contract/
    feature_set_id: fraud-v4
    schema_version: 12
  incident/
    degrade_mode: false
    skip_slow_features: false
```

Note: `aggregation_days` appears in **both** trees intentionally — with a **contract** that they must match at launch. After launch, serving may temporarily lower windows in `incident/degrade_mode` without touching offline training policy.

## Integration — Python offline (training worker)

```python
from kiponos import Kiponos

kiponos = Kiponos.create_for_current_team()
# KIPONOS_PROFILE = ['ml']['fraud']['offline']['features']

def offline_feature_policy() -> dict:
    eng = kiponos.path("offline", "engagement")
    exp = kiponos.path("offline", "experiment")
    return {
        "aggregation_days": eng.get_int("aggregation_days", 7),
        "min_events": eng.get_int("min_events", 3),
        "decay_factor": eng.get_float("decay_factor", 0.95),
        "sample_rate": eng.get_float("sample_rate", 1.0),
        "max_rows": exp.get_int("max_rows_per_epoch", 500_000),
    }

def build_training_frame(raw_df):
    policy = offline_feature_policy()  # local read every epoch
    return compute_engagement_features(
        raw_df,
        window_days=policy["aggregation_days"],
        min_events=policy["min_events"],
        decay=policy["decay_factor"],
        sample_rate=policy["sample_rate"],
    )
```

## Integration — Java online (inference service)

```java
import io.kiponos.sdk.Kiponos;
import org.springframework.stereotype.Service;

@Service
public class FeatureAssembler {

    private final Kiponos kiponos;
    private final FeatureStoreClient featureStore;

    public FeatureAssembler(Kiponos kiponos, FeatureStoreClient featureStore) {
        this.kiponos = kiponos;
        this.featureStore = featureStore;
        kiponos.afterValueChanged(change -> {
            if (change.path().startsWith("online/incident")) {
                log.warn("Serving incident mode changed: {}", change.path());
            }
        });
    }

    public FeatureVector assemble(String userId) {
        var online = kiponos.path("online", "engagement");
        var incident = kiponos.path("online", "incident");
        int windowDays = online.getInt("aggregation_days", 7);
        double missing = online.getDouble("default_missing", 0.0);
        int ttlSec = online.getInt("cache_ttl_sec", 300);

        if (incident.getBool("degrade_mode", false)) {
            windowDays = Math.min(windowDays, 1);
        }
        if (incident.getBool("skip_slow_features", false)) {
            return FeatureVector.minimal(userId, missing);
        }

        return featureStore.fetch(userId, windowDays, ttlSec, missing);
    }
}
```

Contract check at startup (both runtimes):

```java
String servingId = kiponos.path("online", "contract").get("feature_set_id");
// Python trainer logs mismatch if offline contract diverges — fail fast in CI smoke, not prod surprise
```

## Real scenarios

| Event | Single shared `features.yaml` | Split Kiponos trees |
|-------|------------------------------|---------------------|
| Serving latency incident | Edit shared file; training job picks up wrong window next run | `online/incident/degrade_mode` only — offline untouched |
| Experiment new aggregation | Risk serving reads half-baked window | `offline/engagement/aggregation_days` live during trainer |
| Launch fraud model v4 | Manual sync pray | `contract/feature_set_id` must match both profiles |
| Black Friday | Freeze training experiments | Serving ops lowers `cache_ttl_sec` live for fresher signals |

## Performance — why split trees stay fast

- **Two WebSockets, two processes** — trainer and serving never contend on one giant tree
- Local `get_int()` on both sides — O(1), no feature-store RTT for policy reads
- Delta scope is small — incident toggle patches `online/incident`, not offline experiment keys
- Serving cache TTL changes apply on next `assemble()` — no JVM restart
- Training sample rate changes apply on next epoch — no job kill

## Compare to alternatives

| Approach | Independent offline/online tuning | Hot-path read cost |
|----------|-----------------------------------|---------------------|
| Single `features.yaml` in Git | Coupled PRs | Zero until skew |
| Monolithic Redis hash | Possible | RTT × every request / batch |
| Feature store only | Values yes; policy knobs often still external | Store latency |
| Two env var sets per deploy | Rolling restart both fleets | Frozen between deploys |
| **Kiponos split profiles** | **Yes — ACL per tree** | **Memory read each side** |

## When not to use Kiponos for feature config

| Case | Better approach |
|------|-----------------|
| Raw feature values and point-in-time joins | Feature store / warehouse |
| New feature column definition (SQL transform) | Git-reviewed pipeline code |
| Model artifact URI and A/B model weights | Model registry + [LLM serving patterns](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-llm-inference-serving.md) |
| PII column allowlist for GDPR | Legal-reviewed static policy in Git |

## Getting started (15 minutes)

1. [TeamPro at kiponos.io](https://kiponos.io) — create **two** profiles: `['ml']['YOUR_MODEL']['offline']['features']` and `['ml']['YOUR_MODEL']['online']['features']`.
2. Python trainer: `pip install kiponos`, point `KIPONOS_PROFILE` at offline tree, replace hard-coded windows with `offline_feature_policy()`.
3. Java serving: add `io.kiponos:sdk-boot-3`, point profile at online tree, read `cache_ttl_sec` and fallbacks in `FeatureAssembler`.
4. Seed matching `contract/feature_set_id` in both trees; add CI smoke that compares IDs.
5. Game day: enable `online/incident/degrade_mode` in staging during load test — confirm training profile unchanged and serving latency drops **without restart**.

**Further reading:**

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — one governance model, two feature trees. Train offline, serve online, tune both live.*