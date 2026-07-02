---
title: "LLM Token Budget Per User as Live Policy — Cap Inference Spend Without Recycling Python Workers (Python SDK)"
published: false
tags: python, ai, llm, architecture
description: Token limits frozen in constants.py mean usage spikes need worker restarts. Kiponos holds per-tier budgets, burst multipliers, and throttle flags in one live tree — ops tunes caps while FastAPI workers keep running.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-llm-token-budget-per-user.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-arch-llm-token-budget-per-user.jpg
---

Wednesday 16:44 UTC. Finance pings `#ai-platform`: **OpenAI invoice** trending **340% above forecast** — a product team shipped a summarization feature without telling anyone. Your FastAPI inference gateway still enforces `MAX_TOKENS_PER_USER_PER_DAY = 50_000` from `limits.py`, a constant chosen during pilot when eight internal users tested the stack.

By 17:10, three enterprise tenants each burned **180k tokens** in two hours. The ML platform lead wants **immediate per-tier caps** — free at 20k, pro at 100k, enterprise at 500k with burst disabled. Someone proposes a hotfix branch to recycle **24 Uvicorn workers** mid-peak.

The FinOps owner asks:

> "Why does **spend control** require a **deploy** when the gateway already counts tokens on every request?"

Most Python LLM gateways encode token budgets as **three different artifacts**: a spreadsheet tier matrix, module-level constants imported at worker boot, and a Redis counter with no connection to the policy that sets the ceiling. [Kiponos.io](https://kiponos.io) collapses that into **one operational tree** — per-tier daily caps, burst multipliers, and global throttle flags — readable on every inference request with **local `get*()` calls** and adjustable from the dashboard while workers run.

## The problem: token budgets baked into immutable Python constants

A typical gateway enforces limits like this:

```python
# limits.py — imported once at worker boot
MAX_TOKENS_PER_USER_PER_DAY = 50_000
MAX_TOKENS_PER_REQUEST = 4_096
BURST_MULTIPLIER = 1.5

async def check_budget(user_id: str, estimated_tokens: int) -> bool:
    used = await usage_store.get_daily(user_id)
    if used + estimated_tokens > MAX_TOKENS_PER_USER_PER_DAY:
        return False
    return True
```

Budget policy usually lives elsewhere — scattered and static:

```yaml
# config/prod.yaml — requires worker recycle to change
llm:
  tiers:
    free:
      daily_token_cap: 20000
    pro:
      daily_token_cap: 100000
```

Or worse — one global cap because per-tier env vars got messy:

```python
# "We'll add tiers in v2"
MAX_TOKENS_PER_USER_PER_DAY = 50_000
```

The inference path executes **hundreds of requests per minute per tenant**. During a spend spike you need to:

1. Lower **`budget.tiers.free.daily_cap`** before the next billing cycle hemorrhage
2. Flip **`budget.global_throttle`** to shed non-critical routes instantly
3. Disable **`budget.burst.enabled`** for enterprise tenants abusing burst windows

Doing that through a deploy while tokens keep flowing is not FinOps — it is **invoice theater with compound interest**.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Token caps belong in the model router config" | Gateway counts tokens; router never reads the ceiling |
| "We'll alert at 80% budget" | Alerts inform humans; requests keep succeeding |
| "Per-tier limits need a database migration" | Ceilings are policy integers — not relational schema |
| "Redis TTL handles daily reset" | Redis stores usage; caps still live in stale constants |
| "Pilot constants scale to production" | Production has tiers; constants have one integer |

## The architecture insight

**LLM token budgets are operational config, not ML archaeology.** The same knobs your FinOps runbook tells ops to edit — per-tier daily caps, burst multipliers, global throttle — belong in **one live tree** the Python worker already reads on every inference request. Kiponos makes "free tier 20k now" a **dashboard edit**, not a Uvicorn recycle.

## What Kiponos.io is for LLM token budgets

[Kiponos.io](https://kiponos.io) is a real-time configuration hub with Java and Python SDKs. `Kiponos.create_for_current_team()` connects over WebSocket; the profile tree — for example `['llm-gateway']['prod']['budget']` — hydrates into **in-process memory** at worker startup.

When FinOps sets `budget.tiers.free.daily_cap` to `20000`, a **delta** patches only that key. The next `kiponos.path("budget", "tiers", tier).get_int("daily_cap")` on an incoming `/v1/chat` request is a **local memory read** — no HTTP to a config API, no poll loop, no extra Redis round-trip for policy.

`after_value_changed` logs policy flips and can emit metrics to your billing pipeline **without** restarting workers.

No restart. No redeploy. No recycling the worker pool.

## Reference architecture

![Architecture diagram](https://litter.catbox.moe/6ceglb.png)

**Tier matrix documents intent; the tree enforces live caps.** Keep commercial pricing prose in your finance wiki — but the **authoritative token ceilings** live in Kiponos where tightening them takes seconds.

## Config tree — budget, tiers, burst, and throttle

Five folders — `budget`, `tiers`, `burst`, `throttle`, `audit`:

```yaml
budget/
  default_tier: free
  max_tokens_per_request: 4096
  count_prompt_and_completion: true
tiers/
  free/
    daily_cap: 20000
    requests_per_minute: 30
  pro/
    daily_cap: 100000
    requests_per_minute: 120
  enterprise/
    daily_cap: 500000
    requests_per_minute: 600
burst/
  enabled: true
  multiplier: 1.5
  window_minutes: 15
  disabled_tiers: []
throttle/
  global_throttle: false
  throttle_message: "LLM gateway at capacity — retry later"
  shed_routes: ["/v1/summarize", "/v1/embed"]
audit/
  last_cap_change_by: ""
  last_cap_change_at_ms: 0
  emit_denial_metrics: true
```

One tree. One profile path: `['llm-gateway']['prod']['budget']`. Staging spend drills share **identical key layout** — only values differ.

## Python integration: budget gate + live tier caps

```python
import logging
from fastapi import FastAPI, HTTPException, Request
from kiponos import Kiponos

log = logging.getLogger(__name__)
app = FastAPI()

kiponos = Kiponos.create_for_current_team()
# Profile: ['llm-gateway']['prod']['budget'] via KIPONOS_PROFILE env

def _tier_cfg(tier: str):
    return kiponos.path("budget", "tiers", tier)

def effective_daily_cap(tier: str) -> int:
    base = _tier_cfg(tier).get_int("daily_cap", 20000)
    burst = kiponos.path("budget", "burst")
    if burst.get_bool("enabled", True) and tier not in burst.get_list("disabled_tiers", []):
        mult = burst.get_float("multiplier", 1.5)
        return int(base * mult)
    return base

kiponos.after_value_changed(
    lambda change: log.info("LLM budget delta: path=%s value=%s", change.path, change.new_value)
)

@app.middleware("http")
async def global_throttle(request: Request, call_next):
    throttle = kiponos.path("budget", "throttle")
    if throttle.get_bool("global_throttle", False):
        if request.url.path in throttle.get_list("shed_routes", []):
            raise HTTPException(503, throttle.get("throttle_message", "capacity"))
    return await call_next(request)

@app.post("/v1/chat")
async def chat(request: Request, body: ChatRequest):
    user = request.state.user
    tier = user.tier or kiponos.path("budget").get("default_tier", "free")

    max_per_req = kiponos.path("budget").get_int("max_tokens_per_request", 4096)
    estimated = body.estimate_tokens()
    if estimated > max_per_req:
        raise HTTPException(400, "request exceeds max_tokens_per_request")

    used = await usage_store.get_daily(user.id)
    cap = effective_daily_cap(tier)

    if used + estimated > cap:
        if kiponos.path("budget", "audit").get_bool("emit_denial_metrics", True):
            metrics.inc("llm_budget_denied", tier=tier)
        raise HTTPException(429, f"daily token cap exceeded for tier {tier}")

    result = await model_client.complete(body)
    await usage_store.add(user.id, result.total_tokens)
    return result
```

Every `get_int()`, `get_bool()`, and `get_float()` on the inference path is **O(1) local cache** — microseconds, not cross-region config service RTT.

Usage **counts** stay in Redis or your billing store — Kiponos owns the **ceilings** that change when finance rings the alarm.

## Real-world scenarios

| Scenario | Without live budget tree | With Kiponos one-tree LLM policy |
|----------|--------------------------|----------------------------------|
| Runaway summarization feature | Deploy new constants; workers recycle | Dashboard: lower `tiers/free/daily_cap` live |
| Enterprise burst abuse | Manual account suspension | `burst/disabled_tiers: ["enterprise"]` |
| Provider outage — shed load | Code change to drop routes | `throttle/global_throttle: true` + `shed_routes` |
| New commercial tier launch | Env var matrix per deploy | Add `tiers/startup/daily_cap` in dashboard |
| Post-incident restore | Second deploy wave | Reset caps and burst in dashboard |

## Performance: why budget gates must not add network I/O

- **One WebSocket per worker** — not one config fetch per inference request
- **Cap resolution is four local reads** — nanoseconds vs model provider RTT
- **Delta patches** — tightening one tier sends one patch, not full tree reload
- **Usage store unchanged** — Redis still tracks counts; only ceilings move live
- **No GC pressure** from re-parsing YAML on every token count during spikes

In load tests, Kiponos reads are noise on the gateway path; OpenAI/Anthropic network latency dominates.

## Compare to alternatives

| Approach | Mid-spike cap tighten | Hot-path read latency | Single tree for tiers + burst + throttle |
|----------|----------------------|----------------------|------------------------------------------|
| constants.py + worker recycle | No — pool restart | Zero (static) but stale | No — one global integer |
| Per-tier env vars | No — redeploy | Zero after restart | Partial — messy matrix |
| Database policy table | Yes with query | Milliseconds per request | Possible — ORM overhead |
| Redis hash for caps | Yes — but mixed with usage keys | Sub-ms but shared namespace | Possible — schema discipline |
| Feature-flag SaaS | Booleans only | SDK network on evaluation | No — integer caps awkward |
| **Kiponos SDK** | **Yes — dashboard delta** | **Zero (in-process cache)** | **Yes — one profile tree** |

## When not to use Kiponos for LLM token budgets

| Boundary | Better home |
|----------|-------------|
| API keys for OpenAI, Anthropic, Azure OpenAI | Vault / secrets manager — not live dashboard |
| Model selection, temperature, top_p sampling params | Version-controlled prompt registry if Git-reviewed |
| Actual token **usage counts** per user per day | Redis / Postgres billing table — source of truth |
| Invoice reconciliation against provider bills | FinOps warehouse / accounting system |
| Training data pipelines and fine-tune job configs | ML platform GitOps |

## Getting started (15 minutes)

1. [Create TeamPro at kiponos.io](https://kiponos.io) — add profile `['llm-gateway']['prod']['budget']` with `budget`, `tiers`, and `throttle` folders matching the tree above.
2. Add `kiponos` Python SDK to your FastAPI inference gateway.
3. Set `KIPONOS_ID`, `KIPONOS_ACCESS`, and `KIPONOS_PROFILE=['llm-gateway']['prod']['budget']`.
4. Replace `MAX_TOKENS_PER_USER_PER_DAY` with `effective_daily_cap(tier)` using `kiponos.path(...)`.
5. Register `after_value_changed` logging and wire denial metrics to your billing dashboard.
6. Drill: in staging, lower `tiers/pro/daily_cap` and confirm 429 responses increase **without worker restart**. Document key names in your FinOps runbook.

## Further reading

- [Developer Quickstart](https://dev.to/kiponos/kiponosio-developer-quickstart-java-python-and-your-first-live-config-change-3kjo)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
- Related: [RAG chunk top-k live](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-ai-rag-chunk-topk.md)
- Related: [Cost control runtime](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-cost-control-runtime.md)

---

*Kiponos.io — FinOps spreadsheets describe tiers; the tree enforces caps on every request.*