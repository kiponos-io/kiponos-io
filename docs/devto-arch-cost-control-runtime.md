---
title: "Cost Control at Runtime — Autoscale Caps, GPU Budget, and Token Limits for LLM Fleets (Kiponos Python SDK)"
published: true
tags: finops, ai, python, architecture
description: FinOps knobs trapped in Terraform and Helm cannot react when an LLM fleet burns GPU budget at 2 AM. Kiponos feeds autoscale caps, daily spend limits, and per-tenant token ceilings to Python inference workers — live reads, no pod restart.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-cost-control-runtime.md
main_image: https://litter.catbox.moe/8xtald.jpg
cover_image: /home/moshe/work/kiponos-io/docs/devto-cover-arch-cost-control-runtime.jpg
---

Monday 02:18 UTC. The on-call FinOps engineer wakes to a PagerDuty spike — not errors, **spend**. Your LLM inference fleet autoscaled from 8 to 47 GPU nodes in forty minutes after a partner integration went live without a rate limit. CloudWatch shows **$12,400 projected daily burn** against a **$8,500 cap**. The autoscaler HPA max is `maxReplicas: 64` in Helm — written when the cluster was half this size.

Platform lead in the bridge:

> "We have a budget. We have token limits in the API gateway. Why is the GPU fleet still scaling?"

Because **three different systems** own three different knobs: Terraform sets node pool ceilings, Kubernetes HPA sets replica max, and the Python inference worker hard-codes `MAX_TOKENS = 4096` in `serving_config.py`. None of them talk to each other mid-flight. Lowering spend means a **Helm values PR**, a **cluster autoscaler quota ticket**, and a **redeploy of every inference pod** — while GPUs keep spinning.

[Kiponos.io](https://kiponos.io) unifies **FinOps policy** in one live tree that every Python worker, admission controller, and fleet supervisor reads locally on every request. Autoscale caps, GPU budget gates, and token ceilings become **operational parameters** — not semver artifacts.

## The problem: frozen FinOps constants on the hot path

Most LLM fleets scatter cost controls across layers:

```python
# serving_config.py — shipped with the Docker image
MAX_GPU_REPLICAS = 64
DAILY_SPEND_CAP_USD = 8500.0
DEFAULT_MAX_TOKENS = 4096
PREMIUM_MAX_TOKENS = 8192
RPM_PER_TENANT = 200
```

```yaml
# k8s/hpa-llm-inference.yaml
spec:
  maxReplicas: 64
  minReplicas: 4
```

```hcl
# terraform/gpu-pool.tf
max_node_count = 80
```

Your admission path runs at **thousands of requests per second** across a GPU pool. Each generation call reads `max_tokens` before hitting vLLM or TensorRT-LLM. The fleet supervisor polls GPU utilization every 30 seconds and asks the cluster for more replicas. Works until:

- A launch doubles traffic and HPA hits `maxReplicas` **before** FinOps can merge a Helm change
- A runaway agent loop burns **output tokens** because `max_tokens` is compiled in
- Weekend on-call needs **hard stop at $9k** but Terraform apply waits on state lock
- Cheap-model routing should flip to 90% when budget is 85% consumed — routing weights live in a separate feature-flag SaaS

The hot path cannot poll a billing API per token. But **policy constants** should not require a rolling restart across 40 GPU pods while spend accelerates.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Cloud budgets in AWS/GCP console will save us" | Console alerts fire **after** burn; they do not cap `max_tokens` on live workers |
| "HPA maxReplicas is enough" | HPA does not know **daily USD**; it only knows CPU and queue depth |
| "API gateway token limits protect the fleet" | Gateway limits ingress; **internal batch jobs** and sidecars bypass it |
| "We'll scale down tomorrow in Terraform" | Tomorrow's invoice already has tonight's 47 nodes |
| "FinOps belongs in the finance spreadsheet" | Spreadsheet does not reach the Python process holding the GPU |

## The Aha

**Autoscale ceilings, GPU spend caps, and per-tenant token limits are runtime FinOps knobs** — tighten them while inference keeps running. Point your Python fleet at a Kiponos tree; ops sets `max_gpu_replicas: 16` and `default_max_tokens: 1024` in the dashboard; every worker's next request sees the new policy without a pod recycle.

## What Kiponos.io is in an LLM FinOps stack

[Kiponos.io](https://kiponos.io) is a real-time config hub. Each Python inference worker connects once via WebSocket; **delta patches** update an in-memory tree inside the SDK. Every `kiponos.path(...).get_int("max_tokens")` on the generation path is a **local memory read** — no HTTP to the hub, no Redis poll, no S3 fetch.

Profile path for this story:

```
['llm-fleet']['prod']['finops']
```

**WebSocket delta → in-memory tree → local `get*()` on the hot path.** Ops edits `finops/gpu_budget/daily_spend_cap_usd` in the dashboard; connected workers receive the patch in milliseconds. A fleet supervisor reading the same tree can **stop requesting scale-up** when `enforce_hard_stop` flips true — no Helm, no Terraform apply, no inference image rebuild.

No restart. No redeploy. No `@RefreshScope` equivalent in Python.

Pair with [LLM inference serving](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-llm-inference-serving.md) for temperature and routing — this article owns **spend guardrails**.

## Architecture

![Architecture diagram](https://litter.catbox.moe/zz4dtl.png)

**Billing stays in your cloud console.** Kiponos holds **policy** — when to stop admitting, how many replicas the supervisor may request, how many tokens each tenant may emit. Optional: a small FinOps bot writes `current_day_spend_usd` into the tree from CUR data; workers react via `afterValueChanged`.

## Config tree (FinOps policy, five folders)

```yaml
finops/
  autoscale/
    max_gpu_replicas: 16
    min_gpu_replicas: 4
    scale_up_cooldown_seconds: 300
    scale_down_cooldown_seconds: 900
    hard_cap_enabled: true
  gpu_budget/
    daily_spend_cap_usd: 8500
    hourly_burn_alert_usd: 400
    warn_threshold_pct: 85
    enforce_hard_stop: false
    current_day_spend_usd: 0
  tokens/
    default_max_tokens: 2048
    premium_max_tokens: 4096
    rpm_per_tenant: 120
    tpm_per_tenant: 80000
    output_token_multiplier_alert: 1.5
  routing/
    cheap_model_weight: 70
    premium_model_weight: 30
    force_cheap_on_budget_warn: true
  fleet/
    admission_enabled: true
    drain_on_budget_breach: true
    reject_message: "fleet_budget_exceeded"
    supervisor_poll_seconds: 30
```

Eighteen keys across **autoscale**, **gpu_budget**, **tokens**, **routing**, and **fleet** — one tree shape for every Python worker in the pool.

## Integration: admission, generation, and supervisor

```python
import os
import time
from dataclasses import dataclass

from kiponos import Kiponos

os.environ["KIPONOS_PROFILE"] = "['llm-fleet']['prod']['finops']"
kiponos = Kiponos.create_for_current_team()


@dataclass
class GenerateRequest:
    tenant_id: str
    prompt: str
    tier: str  # "default" | "premium"


class FinOpsAdmission:
    """Called before a request enters the GPU queue."""

    def __init__(self, kiponos: Kiponos):
        self._kiponos = kiponos
        self._tenant_rpm: dict[str, list[float]] = {}

    def allow(self, req: GenerateRequest) -> tuple[bool, str]:
        fleet = self._kiponos.path("finops", "fleet")
        budget = self._kiponos.path("finops", "gpu_budget")
        tokens = self._kiponos.path("finops", "tokens")

        if not fleet.get_bool("admission_enabled", True):
            return True, "admission_disabled"

        if budget.get_bool("enforce_hard_stop", False):
            return False, fleet.get("reject_message", "fleet_budget_exceeded")

        cap = budget.get_float("daily_spend_cap_usd", 8500.0)
        spent = budget.get_float("current_day_spend_usd", 0.0)
        warn_pct = budget.get_float("warn_threshold_pct", 85.0)
        if cap > 0 and (spent / cap) * 100.0 >= warn_pct:
            if not tokens.get_bool("force_cheap_on_budget_warn", True):
                pass  # warn only
            elif req.tier == "premium":
                return False, "premium_blocked_budget_warn"

        rpm_limit = tokens.get_int("rpm_per_tenant", 120)
        if not self._check_rpm(req.tenant_id, rpm_limit):
            return False, "rpm_exceeded"

        return True, "ok"

    def _check_rpm(self, tenant_id: str, limit: int) -> bool:
        now = time.time()
        window = self._tenant_rpm.setdefault(tenant_id, [])
        window[:] = [t for t in window if now - t < 60.0]
        if len(window) >= limit:
            return False
        window.append(now)
        return True


def build_generation_params(req: GenerateRequest) -> dict:
    """Hot path — local reads only."""
    tokens = kiponos.path("finops", "tokens")
    routing = kiponos.path("finops", "routing")
    budget = kiponos.path("finops", "gpu_budget")

    max_tokens = (
        tokens.get_int("premium_max_tokens", 4096)
        if req.tier == "premium"
        else tokens.get_int("default_max_tokens", 2048)
    )

    cap = budget.get_float("daily_spend_cap_usd", 8500.0)
    spent = budget.get_float("current_day_spend_usd", 0.0)
    warn_pct = budget.get_float("warn_threshold_pct", 85.0)
    force_cheap = routing.get_bool("force_cheap_on_budget_warn", True)

    if cap > 0 and (spent / cap) * 100.0 >= warn_pct and force_cheap:
        cheap_w = 100
        premium_w = 0
    else:
        cheap_w = routing.get_int("cheap_model_weight", 70)
        premium_w = routing.get_int("premium_model_weight", 30)

    return {
        "max_tokens": max_tokens,
        "cheap_model_weight": cheap_w,
        "premium_model_weight": premium_w,
    }


def desired_replica_count(queue_depth: int, replicas_per_queue_unit: float) -> int:
    """Fleet supervisor — bounded by live autoscale cap."""
    auto = kiponos.path("finops", "autoscale")
    raw = max(auto.get_int("min_gpu_replicas", 4), int(queue_depth * replicas_per_queue_unit))
    if auto.get_bool("hard_cap_enabled", True):
        raw = min(raw, auto.get_int("max_gpu_replicas", 16))
    return raw


def on_budget_change(change) -> None:
    if not change.path.startswith("finops/gpu_budget/"):
        return
    budget = kiponos.path("finops", "gpu_budget")
    cap = budget.get_float("daily_spend_cap_usd", 8500.0)
    spent = budget.get_float("current_day_spend_usd", 0.0)
    if cap > 0 and spent >= cap:
        kiponos.path("finops", "gpu_budget").set("enforce_hard_stop", True)
        kiponos.path("finops", "fleet").set("admission_enabled", False)


kiponos.after_value_changed(on_budget_change)
```

Wire the supervisor loop:

```python
while True:
    poll = kiponos.path("finops", "fleet").get_int("supervisor_poll_seconds", 30)
    depth = measure_queue_depth()
    desired = desired_replica_count(depth, replicas_per_queue_unit=0.25)
    apply_k8s_replica_target(desired)  # never exceeds live max_gpu_replicas
    time.sleep(poll)
```

`get_int()` / `get_float()` / `get_bool()` are **in-process reads** — no hub round-trip per token or per scale decision.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| Partner launch spikes GPU queue | HPA scales to Helm max 64; bill runs away | `max_gpu_replicas: 16` live in 10 seconds |
| 85% daily budget by 6 PM | Email alert; humans debate until 9 PM | `force_cheap_on_budget_warn` shifts routing; `default_max_tokens: 1024` |
| Runaway agent output loop | `MAX_TOKENS=8192` until redeploy | `premium_max_tokens: 2048` dashboard tweak |
| Hard stop at cap | Manual cordon + drain nodes | `enforce_hard_stop: true` + `admission_enabled: false` |
| Weekend scale-down | Terraform PR Monday | `scale_down_cooldown_seconds` + lower `min_gpu_replicas` live |

## Performance

- **Admission check is ~6 local reads** — microseconds vs milliseconds for a billing API call
- **Generation path reads `max_tokens` from cache** — no added latency vs a module constant
- **Supervisor polls policy every 30s** — one tree read batch, not per-pod ConfigMap mount
- **WebSocket deltas are single-key patches** — lowering `max_gpu_replicas` does not reload the full fleet tree
- **No GPU idle time waiting for config** — workers keep inferencing; only numeric policy changes

In production-style vLLM workers, config reads are noise next to attention matmul cost.

## Compare to alternatives

| Approach | Mid-spike replica cap | Token limit change | Fleet-wide consistency |
|----------|----------------------|--------------------|------------------------|
| Helm / HPA YAML | PR + rollout | N/A — not token-aware | Eventual per Deployment |
| Terraform node pool max | State lock + apply | N/A | Cluster-wide but slow |
| Cloud budget alerts | Notify only | N/A | Does not reach Python |
| Redis config poll | Possible | Per-request RTT risk | Depends on client cache |
| Feature flags (LaunchDarkly) | Awkward for numeric caps | Boolean-centric | Separate system from GPU supervisor |
| **Kiponos tree** | **`max_gpu_replicas` dashboard** | **`default_max_tokens` live** | **Same delta to every worker** |

## When not to use Kiponos

| Case | Better tool |
|------|-------------|
| Authoritative cloud billing and invoices | AWS CUR / GCP Billing Export — not config hub |
| GPU hardware procurement and reserved capacity | Finance ERP + cloud commitments |
| Per-request token counting at exact billing precision | Metering service with durable ledger |
| Long-term cost allocation tags and chargeback reports | Cost explorer / FinOps SaaS dashboards |

Use Kiponos for **policy that must reach running Python workers in seconds** — not for replacing your accounting system.

## Getting started (15 minutes)

1. [Create TeamPro at kiponos.io](https://kiponos.io) — profile `['llm-fleet']['prod']['finops']`
2. Export `KIPONOS_ID` and `KIPONOS_ACCESS` from Connect; wire `Kiponos.create_for_current_team()` at worker startup
3. Move `MAX_GPU_REPLICAS`, `DAILY_SPEND_CAP_USD`, and `DEFAULT_MAX_TOKENS` from Python modules into the `finops/*` tree above
4. Add `FinOpsAdmission.allow()` before queue enqueue; read `max_tokens` via `kiponos.path("finops", "tokens")` in `build_generation_params()`
5. Game day: simulate spend past `warn_threshold_pct`, then flip `enforce_hard_stop` live and watch admission reject without restarting pods

## Further reading

- [Developer Quickstart](https://dev.to/kiponos/kiponosio-developer-quickstart-java-python-and-your-first-live-config-change-3kjo)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [LLM inference serving (routing + temperature)](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-llm-inference-serving.md)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — FinOps policy on the GPU hot path, not in tomorrow's Helm PR.*