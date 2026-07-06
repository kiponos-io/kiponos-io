---
title: "MAX_AGENT_STEPS=12 and TOOL_BUDGET_USD=2.50 Were Module Constants — Finance Froze Spend Mid-Incident (Python + Kiponos)"
published: false
tags: python, ai, agents, devops
description: Agent max steps, spend caps, and allowed tools feel like safety constants baked at import. During cost spikes they are operational — Kiponos feeds live agent policy with zero-latency reads per turn.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-ai-agent-tool-budgets.md
main_image: https://files.catbox.moe/n1iaiq.jpg
---

Thursday 11:38 AM. Your internal ops agent handles ticket triage — read Slack, query Postgres, draft Jira comments, call a summarization endpoint. `config.py` ships with `MAX_AGENT_STEPS = 12`, `TOOL_BUDGET_USD = 2.50`, and `ALLOWED_TOOLS = frozenset({"sql_read", "slack_post", "jira_draft"})` because security signed off on that tuple in Q1.

Then a vendor webhook storm creates 3,200 duplicate tickets. The agent loop starts **re-querying** the same incident cluster, burning tool calls and LLM tokens. Finance Slack:

> "Cap every agent run at **$0.40** and **four steps** until we understand the loop. Do not redeploy the agent fleet."

But those caps are `frozenset` and integers imported at process start. Lowering spend means recycling eight FastAPI workers during active triage, dropping in-flight sessions, and re-authenticating tool OAuth tokens. The LLM is fine. **Agent economics** are out of control *right now*.

Here is the Aha:

**Max steps and tool budgets behave like immutable safety rails, but they are operational spend policy for this incident hour.**

You can change `max_steps`, `spend_cap_usd`, and `allowed_tools` **while workers keep serving `/agent/run`** — no redeploy, no restart, no per-turn config poll. The next ReAct iteration already reads the new policy from memory. That is [Kiponos.io](https://kiponos.io).

## The problem — frozen agent budgets on the turn hot path

Python agent frameworks often freeze policy at import:

```python
# config.py
MAX_AGENT_STEPS = 12
TOOL_BUDGET_USD = 2.50
ALLOWED_TOOLS = frozenset({"sql_read", "slack_post", "jira_draft", "web_search"})
```

The agent loop enforces those on every turn:

```python
def run_agent(session_id: str, goal: str, tools: ToolRegistry) -> AgentResult:
    spent = 0.0
    for step in range(MAX_AGENT_STEPS):
        action = llm.plan(goal, history=...)
        if action.tool not in ALLOWED_TOOLS:
            raise ToolDenied(action.tool)
        cost = tools.invoke(action.tool, action.args)
        spent += cost
        if spent > TOOL_BUDGET_USD:
            raise BudgetExceeded(spent)
    ...
```

Finance needs `$0.40` and four steps **now**. Security may want to **disable `web_search`** during the storm. Ops cannot patch running pods. Senior engineers know agent loops can spiral — they do not know caps can move **without recycling the fleet**.

| What teams believe | What production does |
|------------------|---------------------|
| "Step limits protect against infinite loops" | Loop triggers need **tighter** limits live, not next release |
| "Tool allowlists belong in security review" | Incident response needs to **revoke** tools in minutes |
| "Spend caps are finance policy in code" | Finance policy moves when invoices spike hourly |
| "frozenset at import is immutable by design" | Immutability is a deploy bottleneck, not a virtue |

## The Aha — live agent economics while sessions run

Move step caps, spend limits, and tool allowlists into Kiponos:

```yaml
agent/
  limits/
    max_steps: 12
    spend_cap_usd: 2.50
    wall_clock_timeout_sec: 120
  tools/
    allowed: sql_read,slack_post,jira_draft,web_search
    deny_on_budget_warning: web_search
  incident/
    cost_spike_mode: false
    cost_spike_max_steps: 4
    cost_spike_spend_cap_usd: 0.40
    cost_spike_allowed_tools: sql_read,jira_draft
```

Finance enables `incident/cost_spike_mode`. WebSocket delivers a **delta**. The next `run_agent()` reads four steps and `$0.40` — local `get_int()` / `get_float()`, zero network. When the webhook storm clears, disable cost spike mode without redeploy.

## What is Kiponos.io — for agent spend governance

Kiponos syncs operational config to Python agent workers via WebSocket **deltas**. Profile `['ops-agent']['prod']['agent']` holds `limits/max_steps`, `tools/allowed`, and incident overrides.

`kiponos.path("agent", "limits").get_float("spend_cap_usd")` is a **local memory read** inside the ReAct loop — no HTTP per turn, no Redis round trip across thousands of steps. `after_value_changed` can revoke tool permissions immediately when security updates `allowed` — the **next** tool invocation sees the new comma-separated list.

Git keeps **which tools exist in code**. The hub keeps **how many steps and dollars this hour allows**.

## Architecture

![Architecture diagram](https://files.catbox.moe/92lsj0.png)

1. **Connect once** at worker boot.
2. **Snapshot** for `['ops-agent']['prod']['agent']`.
3. **Delta** on dashboard edit — not full policy redeploy.
4. **SDK merges async** on WebSocket thread.
5. **Reads are local** on every agent turn.

## Config tree

```yaml
agent/
  limits/
    max_steps: 12
    spend_cap_usd: 2.50
    wall_clock_timeout_sec: 120
    warn_at_spend_usd: 1.80
  tools/
    allowed: sql_read,slack_post,jira_draft,web_search
    deny_on_budget_warning: web_search
    require_human_approval: slack_post
  incident/
    cost_spike_mode: false
    cost_spike_max_steps: 4
    cost_spike_spend_cap_usd: 0.40
    cost_spike_allowed_tools: sql_read,jira_draft
  telemetry/
    log_denied_tools: true
    emit_budget_metrics: true
```

## Integration — Kiponos-backed agent loop

```python
import logging
import os
import time
from dataclasses import dataclass

from kiponos import Kiponos

log = logging.getLogger(__name__)

os.environ.setdefault("KIPONOS_PROFILE", "['ops-agent']['prod']['agent']")
kiponos = Kiponos.create_for_current_team()


@dataclass(frozen=True)
class AgentPolicy:
    max_steps: int
    spend_cap_usd: float
    wall_clock_timeout_sec: int
    allowed_tools: frozenset[str]
    deny_on_budget_warning: str | None


def _parse_tools(csv: str) -> frozenset[str]:
    return frozenset(t.strip() for t in csv.split(",") if t.strip())


def _load_policy() -> AgentPolicy:
    incident = kiponos.path("agent", "incident")
    if incident.get_bool("cost_spike_mode", False):
        return AgentPolicy(
            max_steps=incident.get_int("cost_spike_max_steps", 4),
            spend_cap_usd=incident.get_float("cost_spike_spend_cap_usd", 0.40),
            wall_clock_timeout_sec=kiponos.path("agent", "limits").get_int("wall_clock_timeout_sec", 120),
            allowed_tools=_parse_tools(incident.get("cost_spike_allowed_tools", "sql_read,jira_draft")),
            deny_on_budget_warning=kiponos.path("agent", "tools").get("deny_on_budget_warning"),
        )
    limits = kiponos.path("agent", "limits")
    tools = kiponos.path("agent", "tools")
    return AgentPolicy(
        max_steps=limits.get_int("max_steps", 12),
        spend_cap_usd=limits.get_float("spend_cap_usd", 2.50),
        wall_clock_timeout_sec=limits.get_int("wall_clock_timeout_sec", 120),
        allowed_tools=_parse_tools(tools.get("allowed", "sql_read,slack_post,jira_draft")),
        deny_on_budget_warning=tools.get("deny_on_budget_warning"),
    )


def _on_policy_change(change) -> None:
    if not str(change.path).startswith("agent/"):
        return
    policy = _load_policy()
    log.warning(
        "Agent policy updated: max_steps=%s spend_cap=%.2f tools=%s (trigger=%s)",
        policy.max_steps,
        policy.spend_cap_usd,
        sorted(policy.allowed_tools),
        change.path,
    )


kiponos.after_value_changed(_on_policy_change)


class BudgetExceeded(Exception):
    pass


class ToolDenied(Exception):
    pass


def run_agent(session_id: str, goal: str, tools, llm) -> dict:
    policy = _load_policy()
    spent = 0.0
    history = []
    deadline = time.monotonic() + policy.wall_clock_timeout_sec
    warn_at = kiponos.path("agent", "limits").get_float("warn_at_spend_usd", 1.80)
    effective_allowed = set(policy.allowed_tools)

    for step in range(policy.max_steps):
        if time.monotonic() > deadline:
            break

        action = llm.plan(goal, history=history)
        tool_name = action["tool"]

        if tool_name not in effective_allowed:
            if kiponos.path("agent", "telemetry").get_bool("log_denied_tools", True):
                log.info("Denied tool %s for session %s", tool_name, session_id)
            raise ToolDenied(tool_name)

        if spent >= warn_at and policy.deny_on_budget_warning:
            effective_allowed.discard(policy.deny_on_budget_warning)

        cost = tools.invoke(tool_name, action.get("args", {}))
        spent += cost
        history.append({"step": step, "tool": tool_name, "cost": cost})

        if spent > policy.spend_cap_usd:
            if kiponos.path("agent", "telemetry").get_bool("emit_budget_metrics", True):
                log.warning("Budget exceeded session=%s spent=%.2f cap=%.2f", session_id, spent, policy.spend_cap_usd)
            raise BudgetExceeded(spent)

    return {"steps": len(history), "spent_usd": spent, "history": history}
```

Webhook storm? Finance enables `cost_spike_mode`. **Next agent session** hits four steps and forty cents. Security trims `allowed` to drop `web_search` — **next turn** enforces the new list.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| Vendor duplicate ticket flood | Redeploy to lower step cap | `cost_spike_max_steps: 4` live |
| Runaway web_search loop | Kill workers, lose sessions | Remove `web_search` from `allowed` |
| Month-end budget freeze | Emergency code freeze | `spend_cap_usd: 0.25` dashboard |
| Gradual trust rebuild | Another image roll | Disable `cost_spike_mode` |
| Per-tenant enterprise cap | Fork constants per customer | Hub profile `ops-agent/tenant-acme` |

See also [model routing weights](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-ai-model-routing-weights.md) when agent loops spike LLM cost on the planner model.

## Compare to alternatives

| Approach | Lower spend cap during spike | Per-turn overhead |
|----------|------------------------------|-------------------|
| `config.py` constants | PR + worker recycle | Zero (frozen) |
| Env var at boot | Rolling restart | Zero after restart |
| Poll Redis per turn | Yes | RTT × steps |
| Feature flag (boolean only) | Toggle modes | No float caps |
| **Kiponos SDK** | **Dashboard, seconds** | **Memory read** |

## Performance — why agent teams care

- **`get_float("spend_cap_usd")` is in-process** — safe inside tight ReAct loops
- **One WebSocket per worker** — not config HTTP per turn
- **Policy reload once per `run_agent()`** — not per tool invocation
- **`after_value_changed` logs transitions** — audit without metrics pipeline lag
- **Comma-separated tool lists patch via delta** — no full allowlist blob reload

## When not to use Kiponos for agent policy

| Case | Better approach |
|------|-----------------|
| Tool implementation code (new `jira_draft` handler) | Code review + deployment |
| OAuth client secrets for Slack / Jira | Vault |
| LLM model selection (`gpt-4o` vs `mini`) | Versioned model registry + deploy |
| Replacing ReAct with graph-based orchestration | Architecture migration |
| Hard compliance ban on `sql_write` forever | Immutable constant in code |

## Getting started (15 minutes)

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['ops-agent']['prod']['agent']`.
2. `pip install kiponos` in your agent service.
3. Move `MAX_AGENT_STEPS`, `TOOL_BUDGET_USD`, and `ALLOWED_TOOLS` into the hub tree.
4. Wire `_load_policy()` at the top of `run_agent()`.
5. Add `cost_spike_mode` incident keys for finance drills.
6. Rehearsal: enable cost spike mode in staging, confirm tighter caps **without worker restart**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — agent step caps are spend policy for this hour, not frozenset forever.*