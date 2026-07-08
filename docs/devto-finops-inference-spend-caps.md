---
title: "Inference Spend Caps — Per-Tier Token Budgets FinOps Controls Live (Python SDK)"
published: false
tags: python, ai, finops, llm
description: Complements LLM token budget article — focuses on spend caps tied to billing tiers and hard stops on the gateway hot path.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-finops-inference-spend-caps.md
main_image: https://files.catbox.moe/n1iaiq.jpg
---

Monday 14:55 UTC. Finance drops a Slack bomb in `#ai-finops`: **Contoso Ltd** — your largest enterprise inference tenant — is at **$18,400 MTD** against a **$12,000** contracted run rate. The usage graph went vertical after they pointed a document-ingestion pipeline at `/v1/embed` without telling anyone.

ML platform lead **Dana Okonkwo** is on a call with customer AE **Victor Lam** and FinOps analyst **Sofia Ruiz**. Victor cannot pause the contract today. Sofia needs a **hard daily spend stop** before the invoice crosses **$20,000** and triggers executive escalation:

> "Set Contoso's `daily_spend_cap_usd` to **$800** for the rest of this week. Do not touch the other enterprise tenants. I am not recycling **28 Uvicorn workers** while embeddings keep burning GPU."

The inference gateway still enforces `DAILY_SPEND_CAP_USD = 500` from `spend_policy.py` — a single integer from the pilot when one tenant tested the stack. Per-tenant caps live in a spreadsheet Sofia updates manually; the gateway never reads it.

The FinOps owner asks what everyone is thinking:

> "We already compute estimated dollars on every request. Why does **enterprise spend control** require a **deploy** when the number we need to change is a float?"

Most Python inference gateways treat spend caps as **bootstrap config**: module-level constants, a billing warehouse that updates hourly, and alerts that fire after the damage is done. [Kiponos.io](https://kiponos.io) collapses per-tenant `daily_spend_cap_usd`, tier defaults, and emergency hard-stop flags into **one operational tree** — readable on every inference request with local `get*()` calls and adjustable from the dashboard while workers keep running.

## The problem — daily_spend_cap_usd baked into static config

A typical inference gateway enforces spend like this:

```python
# spend_policy.py — imported once at worker boot
DAILY_SPEND_CAP_USD = 500.0
DEFAULT_ENTERPRISE_CAP_USD = 2000.0

async def check_spend_cap(tenant_id: str, estimated_usd: float) -> bool:
    mtd = await billing_store.get_mtd_spend(tenant_id)
    if mtd + estimated_usd > DAILY_SPEND_CAP_USD:
        return False
    return True
```

Spend policy usually lives elsewhere — scattered and deploy-bound:

```yaml
# config/prod.yaml — requires worker recycle to change
inference:
  spend:
    daily_spend_cap_usd: 500
    enterprise:
      contoso:
        daily_spend_cap_usd: 2000
```

Or worse — one global cap because per-tenant env vars got messy:

```python
# "We'll add per-tenant caps in v2"
DAILY_SPEND_CAP_USD = 500.0
```

The inference path executes **hundreds of requests per minute per tenant**. During a forecast breach you need to:

1. Lower **`tenants/contoso/daily_spend_cap_usd`** before the next embedding batch hemorrhages margin
2. Keep **`tiers/enterprise/daily_spend_cap_usd`** as the default for well-behaved accounts
3. Flip **`posture/hard_stop_enabled`** to return 402 on any request that would exceed cap

Doing that through a deploy while GPU minutes keep accumulating is not FinOps — it is **invoice theater with compound interest**.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Billing warehouse is source of truth" | Warehouse updates hourly; gateway enforces **stale** caps at request time |
| "We'll alert at 80% of forecast" | Alerts inform humans; requests keep succeeding at old ceilings |
| "Per-tenant caps belong in the CRM" | CRM documents contracts; gateway never reads Salesforce on the hot path |
| "Token budgets are enough" | Tokens and **dollars** diverge when models and batch sizes change |
| "Enterprise tenants self-police" | Document-ingestion pipelines do not read your FinOps spreadsheet |

## The Aha

**daily_spend_cap_usd is operational config** — it changes during forecast breaches, contract negotiations, and margin incidents. It belongs in a **live tree** the gateway already reads with `get_float()`, not in a constant imported at worker boot.

## What Kiponos.io is for inference spend caps

[Kiponos.io](https://kiponos.io) is a real-time configuration hub with Java and Python SDKs. `Kiponos.create_for_current_team()` connects over WebSocket; the profile tree — for example `['inference']['prod']['spend']` — hydrates into **in-process memory** at worker startup.

When Sofia sets `tenants/contoso/daily_spend_cap_usd` to `800`, a **delta** patches only that key. The next `kiponos.path("tenants", tenant_id).get_float("daily_spend_cap_usd")` on an incoming `/v1/embed` request is a **local memory read** — no HTTP to a config API, no poll loop, no extra Redis round-trip for policy.

`after_value_changed` logs cap flips and can emit metrics to your billing pipeline **without** restarting workers.

No restart. No redeploy. No recycling the worker pool.

Honest boundary: Kiponos does **not** replace your billing warehouse for invoice reconciliation, CRM for contract values, or Vault for API keys. It owns **runtime spend ceilings** Python gateways read on every inference request.

## Architecture

![Architecture diagram](https://files.catbox.moe/plnzxs.png)

**CRM documents contracted run rates; authoritative daily ceilings live in Kiponos** where tightening one tenant takes seconds.

## Config tree — tiers, tenants, posture, routes, and audit

Five folders — `defaults`, `tiers`, `tenants`, `posture`, `audit`:

```yaml
defaults/
  daily_spend_cap_usd: 50.0
  fallback_tier: free
  enforce_per_tenant_override: true
  count_prompt_and_completion_cost: true
tiers/
  free/
    daily_spend_cap_usd: 25.0
    enabled: true
  pro/
    daily_spend_cap_usd: 200.0
    enabled: true
  enterprise/
    daily_spend_cap_usd: 2000.0
    enabled: true
tenants/
  contoso/
    daily_spend_cap_usd: 2000.0
    tier: enterprise
    hard_stop: false
  northwind/
    daily_spend_cap_usd: 1500.0
    tier: enterprise
    hard_stop: false
  fabrikam/
    daily_spend_cap_usd: 400.0
    tier: pro
    hard_stop: false
posture/
  hard_stop_enabled: true
  hard_stop_message: "Daily inference spend cap exceeded — contact your account team"
  shed_routes: ["/v1/embed", "/v1/batch"]
audit/
  last_cap_change_by: ""
  last_cap_change_at_ms: 0
  emit_denial_metrics: true
```

One tree. One profile path: `['inference']['prod']['spend']`. Staging spend drills share **identical key layout** — only values differ.

## Python integration — per-tenant spend cap gate + hard-stop posture

```python
import logging
from fastapi import FastAPI, HTTPException, Request
from kiponos import Kiponos

log = logging.getLogger(__name__)
app = FastAPI()

kiponos = Kiponos.create_for_current_team()
# Profile: ['inference']['prod']['spend'] via KIPONOS_PROFILE env

def daily_spend_cap_usd(tenant_id: str) -> float:
    defaults = kiponos.path("spend", "defaults")
    base = defaults.get_float("daily_spend_cap_usd", 50.0)

    tenant = kiponos.path("spend", "tenants", tenant_id)
    if tenant.exists():
        tier = tenant.get("tier", defaults.get("fallback_tier", "free"))
        tier_cfg = kiponos.path("spend", "tiers", tier)
        if tier_cfg.get_bool("enabled", True):
            base = tier_cfg.get_float("daily_spend_cap_usd", base)
        if defaults.get_bool("enforce_per_tenant_override", True):
            base = tenant.get_float("daily_spend_cap_usd", base)
    return base

kiponos.after_value_changed(
    lambda change: log.info("Spend cap delta: path=%s value=%s", change.path, change.new_value)
)

@app.middleware("http")
async def spend_hard_stop(request: Request, call_next):
    posture = kiponos.path("spend", "posture")
    if posture.get_bool("hard_stop_enabled", True):
        if request.url.path in posture.get_list("shed_routes", []):
            tenant_id = request.state.tenant_id
            tenant = kiponos.path("spend", "tenants", tenant_id)
            if tenant.exists() and tenant.get_bool("hard_stop", False):
                raise HTTPException(402, posture.get("hard_stop_message", "cap exceeded"))
    return await call_next(request)

@app.post("/v1/chat")
async def chat(request: Request, body: ChatRequest):
    tenant_id = request.state.tenant_id
    est_usd = estimate_request_cost(body)

    mtd = await billing_store.get_mtd_spend(tenant_id)
    cap = daily_spend_cap_usd(tenant_id)

    if mtd + est_usd > cap:
        if kiponos.path("spend", "audit").get_bool("emit_denial_metrics", True):
            metrics.inc("inference_spend_cap_denied", tenant=tenant_id)
        raise HTTPException(402, f"daily spend cap ${cap:.2f} would be exceeded")

    result = await model_client.complete(body)
    await billing_store.add_spend(tenant_id, billing.actual_cost(result))
    return result

@app.post("/v1/embed")
async def embed(request: Request, body: EmbedRequest):
    tenant_id = request.state.tenant_id
    est_usd = estimate_embed_cost(body)
    mtd = await billing_store.get_mtd_spend(tenant_id)
    cap = daily_spend_cap_usd(tenant_id)

    if mtd + est_usd > cap:
        raise HTTPException(402, f"daily spend cap ${cap:.2f} would be exceeded")

    return await embed_client.embed(body)
```

Every `get_float()`, `get_bool()`, and `get()` on the inference path is **O(1) local cache** — microseconds, not cross-region config service RTT.

**MTD usage counts** stay in Redis or your billing store — Kiponos owns the **ceilings** that change when finance rings the alarm.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| Enterprise tenant exceeds forecast — lower cap before finance escalation | Hotfix branch + Uvicorn recycle | Dashboard: `tenants/contoso/daily_spend_cap_usd: 800` live |
| Embedding pipeline margin bleed | Manual route disable in code | `tenants/contoso/hard_stop: true` + `posture/shed_routes` |
| New enterprise onboarding | Env var matrix per deploy | Add `tenants/newco/` subtree in dashboard |
| Pro tier promotion weekend | Deploy new tier constants | Raise `tiers/pro/daily_spend_cap_usd` live |
| Post-negotiation restore | Second deploy to reset caps | Reset tenant subtree in one edit |

## Performance — hot path economics on inference spend gates

- **Cap resolution per request** — four local reads (defaults, tenant, tier, posture); no HTTP on money path
- **Per-tenant nesting** — enterprise accounts as folders; no `CONTOSO_DAILY_CAP` env var sprawl
- **Delta updates** — tightening one tenant sends one patch; neighbors unchanged
- **Usage store unchanged** — billing Redis still tracks MTD; only ceilings move live
- **One WebSocket per worker** — background sync; hot path never blocks on config API RTT
- **Complements token budgets** — [LLM token budget article](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-llm-token-budget-per-user.md) caps tokens; this article caps **dollars per tenant**

## Compare to alternatives

| Approach | Mid-forecast tenant cap lower | Hot-path read latency | Tier + tenant nesting |
|----------|------------------------------|----------------------|------------------------|
| spend_policy.py + worker recycle | Poor — pool restart | Zero (static) but stale | No — one global float |
| Per-tenant env vars | Poor — redeploy | Zero after restart | Partial — messy matrix |
| Database policy table | Yes with query | Milliseconds per request | Possible — ORM overhead |
| Billing warehouse alerts only | No — informs after breach | N/A | Warehouse not on hot path |
| Feature-flag SaaS | Booleans only | SDK network on evaluation | Poor for per-tenant floats |
| **Kiponos live hub** | **Seconds — dashboard delta** | **Local get*()** | **First-class folder tree** |

## When not to use Kiponos

| Case | Use instead |
|------|-------------|
| Cloud provider list prices and enterprise contracts | Finance wiki + procurement |
| API keys for OpenAI, Anthropic, Azure | Vault / secret manager |
| Immutable invoice line items and GL posting | Billing warehouse — not live config |
| Model selection and prompt templates | Version-controlled prompt registry |
| Per-request GPU $/req ceilings | [GPU dollars per request](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-finops-gpu-dollars-per-request.md) tree |

## Getting started (15 minutes)

1. Sign up at [kiponos.io](https://kiponos.io) (TeamPro).
2. Create profile path `['inference']['prod']['spend']`.
3. Add `tiers/enterprise/daily_spend_cap_usd`, `tenants/contoso/daily_spend_cap_usd`, and wire `daily_spend_cap_usd()` in your gateway hot path.
4. `uvicorn main:app` — confirm log shows WebSocket handshake.
5. Lower `tenants/contoso/daily_spend_cap_usd` in dashboard; send a test request — cap enforced **without** worker restart.
6. Drill: enable `tenants/contoso/hard_stop` and confirm `/v1/embed` returns 402.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [LLM token budget per user](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-llm-token-budget-per-user.md)
- [GPU dollars per request](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-finops-gpu-dollars-per-request.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*daily_spend_cap_usd belongs in the live ops tree — not in constants that mock your FinOps team during the next enterprise forecast breach.*