---
title: "PRIMARY_WEIGHT=0.85 Lived in routing.py — Primary Model Degraded and We Could Not Shift Traffic Live (Python + Kiponos)"
published: false
tags: python, ai, llm, devops
description: Model routing weights and shadow traffic feel like release-time constants. When a primary endpoint degrades, routing is operational — Kiponos feeds live inference policy with zero-latency reads per request.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-ai-model-routing-weights.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-ai-model-routing-weights.jpg
---

Saturday 6:52 PM. Your Python inference gateway routes chat traffic between `gpt-4o` (primary) and `gpt-4o-mini` (fallback). `routing.py` hard-codes `PRIMARY_WEIGHT = 0.85`, `FALLBACK_WEIGHT = 0.15`, and `SHADOW_TRAFFIC_PCT = 0.05` for the candidate `claude-sonnet` endpoint you are evaluating quietly.

At 7:10 PM, primary latency p99 jumps from 800ms to 4.2s — provider degradation, not your code. Error budget bleeds. On-call posts:

> "Shift **90% to fallback** now. Keep **5% shadow** on the new model for comparison. No deploy — GPUs are hot."

But weights are floats in a module imported at Uvicorn boot. Changing them means rolling six inference pods, cold-starting model warmups, and dropping active SSE streams. The fallback model is healthy. **Routing policy** is frozen at 85/15 while customers time out.

Here is the Aha:

**Traffic weights behave like canary config checked into Git, but they are operational routing policy for this provider minute.**

You can change `primary_weight`, `fallback_weight`, and `shadow_traffic_pct` **while workers keep serving `/v1/chat`** — no redeploy, no restart, no per-request routing service call. The next `select_backend()` already reads new weights from memory. That is [Kiponos.io](https://kiponos.io).

## The problem — frozen routing weights on the inference hot path

```python
# routing.py — loaded once at import
PRIMARY_MODEL = "gpt-4o"
FALLBACK_MODEL = "gpt-4o-mini"
SHADOW_MODEL = "claude-sonnet"
PRIMARY_WEIGHT = 0.85
FALLBACK_WEIGHT = 0.15
SHADOW_TRAFFIC_PCT = 0.05
```

Every chat request samples from those weights:

```python
import random

def select_backend(request_id: str) -> str:
    r = random.random()
    if r < SHADOW_TRAFFIC_PCT:
        return SHADOW_MODEL
    if r < SHADOW_TRAFFIC_PCT + PRIMARY_WEIGHT * (1 - SHADOW_TRAFFIC_PCT):
        return PRIMARY_MODEL
    return FALLBACK_MODEL
```

Provider degradation needs 10/90 primary/fallback **now**, with shadow held at 5%. ML cannot patch running pods. Teams **know** routing should be fluid during outages — they do not know weights can move **without recycling inference workers**.

| What teams believe | What production does |
|------------------|---------------------|
| "Weights are set at canary launch" | Provider incidents demand instant reshaping |
| "Shadow traffic belongs in a feature branch" | Shadow % is **observability policy** during degradation |
| "We'll update routing in Monday's release" | Error budget burns Saturday night |
| "Env vars at pod boot are enough" | Boot-time config still needs rolling restart |

## The Aha — live model routing while streams run

```yaml
routing/
  models/
    primary: gpt-4o
    fallback: gpt-4o-mini
    shadow: claude-sonnet
  weights/
    primary_weight: 0.85
    fallback_weight: 0.15
    shadow_traffic_pct: 0.05
  degradation/
    auto_shift_enabled: false
    degraded_primary_weight: 0.10
    degraded_fallback_weight: 0.85
    preserve_shadow_pct: 0.05
```

On-call enables `degradation/auto_shift_enabled`. Dashboard sets `degraded_primary_weight: 0.10`. **Next request** samples the new distribution — local reads, zero network.

## What is Kiponos.io — for inference routing

Kiponos holds routing policy in memory for profile `['inference']['prod']['routing']`. `kiponos.path("routing", "weights").get_float("primary_weight")` is a **local read** inside `select_backend()` — critical when you serve 2,000 RPS and cannot poll a routing control plane per request.

`after_value_changed` logs weight transitions for postmortems — who shifted traffic when, without parsing deploy logs. Git declares **which models your code supports**. The hub declares **what percentage hits each endpoint right now**.

## Architecture

![Architecture diagram](https://litter.catbox.moe/7k3sgo.png)

1. **Connect once** at Uvicorn startup.
2. **Snapshot** for `['inference']['prod']['routing']`.
3. **Delta** on weight edit.
4. **Async merge** on WebSocket thread.
5. **Local `get_float()`** per routing decision.

## Config tree

```yaml
routing/
  models/
    primary: gpt-4o
    fallback: gpt-4o-mini
    shadow: claude-sonnet
  weights/
    primary_weight: 0.85
    fallback_weight: 0.15
    shadow_traffic_pct: 0.05
    normalize_on_read: true
  degradation/
    auto_shift_enabled: false
    degraded_primary_weight: 0.10
    degraded_fallback_weight: 0.85
    preserve_shadow_pct: 0.05
  fallback/
    trigger_on_primary_timeout_ms: 3000
    hard_fallback_mode: false
    hard_fallback_weight: 1.0
```

## Integration — Kiponos-backed routing

```python
import logging
import os
import random
from dataclasses import dataclass

from kiponos import Kiponos

log = logging.getLogger(__name__)

os.environ.setdefault("KIPONOS_PROFILE", "['inference']['prod']['routing']")
kiponos = Kiponos.create_for_current_team()


@dataclass(frozen=True)
class RoutingPolicy:
    primary: str
    fallback: str
    shadow: str
    primary_weight: float
    fallback_weight: float
    shadow_pct: float


def _normalize(primary: float, fallback: float) -> tuple[float, float]:
    total = primary + fallback
    if total <= 0:
        return 0.5, 0.5
    return primary / total, fallback / total


def _load_policy() -> RoutingPolicy:
    models = kiponos.path("routing", "models")
    deg = kiponos.path("routing", "degradation")
    weights = kiponos.path("routing", "weights")

    if deg.get_bool("auto_shift_enabled", False):
        pw = deg.get_float("degraded_primary_weight", 0.10)
        fw = deg.get_float("degraded_fallback_weight", 0.85)
        shadow = deg.get_float("preserve_shadow_pct", 0.05)
    else:
        pw = weights.get_float("primary_weight", 0.85)
        fw = weights.get_float("fallback_weight", 0.15)
        shadow = weights.get_float("shadow_traffic_pct", 0.05)

    if weights.get_bool("normalize_on_read", True):
        pw, fw = _normalize(pw, fw)

    hard = kiponos.path("routing", "fallback")
    if hard.get_bool("hard_fallback_mode", False):
        pw = 0.0
        fw = hard.get_float("hard_fallback_weight", 1.0)

    return RoutingPolicy(
        primary=models.get("primary", "gpt-4o"),
        fallback=models.get("fallback", "gpt-4o-mini"),
        shadow=models.get("shadow", "claude-sonnet"),
        primary_weight=pw,
        fallback_weight=fw,
        shadow_pct=shadow,
    )


def _on_policy_change(change) -> None:
    if not str(change.path).startswith("routing/"):
        return
    p = _load_policy()
    log.warning(
        "Routing policy: primary=%.2f fallback=%.2f shadow=%.2f (trigger=%s)",
        p.primary_weight,
        p.fallback_weight,
        p.shadow_pct,
        change.path,
    )


kiponos.after_value_changed(_on_policy_change)


def select_backend(request_id: str) -> str:
    policy = _load_policy()
    r = random.random()

    if r < policy.shadow_pct:
        return policy.shadow

    # Remaining traffic splits between primary and fallback
    adjusted = (r - policy.shadow_pct) / max(1e-9, 1.0 - policy.shadow_pct)
    if adjusted < policy.primary_weight:
        return policy.primary
    return policy.fallback


def build_chat_payload(prompt: str) -> dict:
    backend = select_backend(request_id="inline")
    cfg = kiponos.path("routing", "models")
    model = {
        cfg.get("primary", "gpt-4o"): cfg.get("primary", "gpt-4o"),
        cfg.get("fallback", "gpt-4o-mini"): cfg.get("fallback", "gpt-4o-mini"),
        cfg.get("shadow", "claude-sonnet"): cfg.get("shadow", "claude-sonnet"),
    }[backend]
    return {"model": model, "messages": [{"role": "user", "content": prompt}]}
```

Provider degrades? Enable `auto_shift_enabled`. **Next SSE stream** routes 85% fallback. Primary recovers? Disable degradation mode — weights snap back without pod restart.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| Primary p99 at 4s | Rolling deploy to change weights | `degraded_fallback_weight: 0.85` live |
| Shadow model evaluation week | Second deployment track | Hold `shadow_traffic_pct: 0.05` independently |
| Total primary outage | Panic hardcode + hotfix PR | `hard_fallback_mode: true` |
| Cost optimization Friday | Static 85/15 overpays primary | Shift to 40/60 from dashboard |
| A/B new model version | Two images | Hub profile `inference/model-v2-canary` |

Pair with [LLM inference serving](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-llm-inference-serving.md) for temperature and token limits on the same hot path.

## Compare to alternatives

| Approach | Shift 90% fallback during outage | Per-request overhead |
|----------|----------------------------------|----------------------|
| `routing.py` constants | PR + rolling restart (15+ min) | Zero (frozen) |
| Kubernetes traffic split | Ingress change — coarse | N/A at app layer |
| Poll Redis for weights | Fast dashboard | RTT × RPS |
| Dedicated routing sidecar | Flexible | Extra hop + ops burden |
| **Kiponos SDK** | **Dashboard delta (seconds)** | **Memory read** |

## Performance — why inference gateways care

- **Weight reads are O(1)** — safe at thousands of RPS
- **One WebSocket per worker** — not HTTP to routing control plane
- **`_load_policy()` once per request** — three floats and three strings
- **Normalization runs in-process** — no floating-point drift across pods if hub is source of truth
- **Shadow % independent of primary/fallback** — ops can tune observability without redeploy

## When not to use Kiponos for model routing

| Case | Better approach |
|------|-----------------|
| Adding a new model adapter class in code | Deployment |
| GPU node pool scaling | Cluster autoscaler |
| TLS certs and provider API keys | Vault |
| Replacing weighted random with learned routing | Architecture project |
| Multi-region active-active ingress weights | Global load balancer + GitOps |

## Getting started (15 minutes)

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['inference']['prod']['routing']`.
2. Move `PRIMARY_WEIGHT`, `FALLBACK_WEIGHT`, and `SHADOW_TRAFFIC_PCT` out of `routing.py`.
3. Implement `_load_policy()` + `select_backend()` with local Kiponos reads.
4. Add `degradation/` keys for provider incident drills.
5. Rehearsal: enable `auto_shift_enabled` in staging, verify traffic skew **without pod restart**.
6. Document boundary: code owns model clients; hub owns **live traffic percentages**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — routing weights are which model serves this request, not routing.py forever.*