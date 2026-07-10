---
main_image: https://files.catbox.moe/dlkkmj.jpg
title: "I Thought Agents Needed a Daemon — Then Kiponos Forced a Mind Shift (Python SDK + MCP + Skill)"
published: false
tags: python, devops, architecture, ai, mcp
description: Building a live control plane for AI agents, I reached for systemd. The right abstraction was Skill + MCP + a small Python SDK — WebSocket deltas to the same tree as the dashboard. Onboarding, package design, and what changed in my mental model.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-agent-live-config-mcp-skill-mindshift.md
---

Tuesday night, pipeline green: articles publishing every two hours, Crunchbase press refs in sync, WhatsApp pings after each cycle. I wanted the **same numbers** on a live dashboard — queue progress, last article URL, Crunchbase rank — without restarting anything when they changed.

I already had a **Skill** that told the agent *how* to talk to Kiponos. I wrote a thin Python client. I set keys. The dashboard updated **without a page refresh**. That part still feels like cheating.

Then I wanted **bidirectional** control: if a human flipped `waves-paused` on the web UI, the publisher should stop. My first instinct as a systems engineer was pure 2014:

> “I’ll install a **systemd user service** that keeps the SDK connected and runs handlers forever.”

It worked. It was also the wrong product shape for **agentic** software.

This article is the mind shift — and the free **Agent Kit** we shipped so any agent (Grok, Cursor, Claude, …) can install the same capability: **Skill + MCP + Python SDK 0.2**.

## The wrong abstraction — “SDK daemon”

Kiponos is a **real-time config hub**: one nested tree per profile, WebSocket/STOMP to the server, React dashboard on the same tree. Java apps have used this for years on hot paths (`get` is local after bootstrap; updates arrive as deltas).

Agents are different processes:

| Process | Lifetime | What it needs |
|---------|----------|---------------|
| Interactive agent session | Minutes to hours | Skill instructions + tools **now** |
| Overnight wave publisher | Days | Pause flags, status writes |
| Human on dashboard | Continuous | Instant visual feedback |

A **Skill** alone dies when the session ends. That is expected.  
A **raw SDK** is correct but every agent re-implements connect, auth errors, and path conventions.

So I created `kiponos-control-watcher.service`. The agent “had a daemon.” Handlers ran. I felt clever.

Then the user asked the question that unblocked the design:

> Why a service? The SDK already has a listener for value changed.

They were right about the **SDK**. They also pointed at the missing standard piece:

> The SDK is not only a skill — the skill tells you how to operate it. A **Kiponos MCP** is the long-lived process you instinctively wanted.

## The mind shift

| Old mental model | New mental model |
|------------------|------------------|
| Skill embeds connection logic | Skill = playbook + onboarding |
| systemd “keeps SDK alive” | **MCP** keeps the client alive |
| Agent restarts = everything dies | MCP process outlives a single turn |
| Dashboard is “admin UI” | Dashboard is a **peer** on the same bus |

Kiponos innovation is not “another remote config REST API.” It is **shared live state** among humans, JVMs, Python workers, and now agents — with the same delta stream. Ingesting that takes time because it collapses three categories we usually separate: *config file*, *feature flag service*, and *message bus*.

Once you see it, the agent kit almost designs itself:

```
Agent  ──tools──►  Kiponos MCP  ──SDK──►  kiponos.io  ◄──  Dashboard
                     (stdio)              WebSocket
```

No custom unit file. No reinventing lifecycle for every host (Grok, Cursor, Claude Code all speak MCP).

## What we built — free Agent Kit

Open source under the [kiponos-io](https://github.com/kiponos-io/kiponos-io) repo: directory **`agent-kit/`**.

### 1. Python SDK 0.2 (`kiponos`)

Clean surface informed by real agent work (not a scaffold):

```python
from kiponos import Kiponos

with Kiponos.connect() as k:  # env: KIPONOS_ID, KIPONOS_ACCESS, KIPONOS
    k.set("marketing/press/dev-to-crunch/queue-progress", "77/110")
    print(k.get("marketing/press/dev-to-crunch/queue-progress"))

    def on_change(key, value, folders, source, delta):
        # Heavy work: enqueue — never block the STOMP listener
        print("delta", "/".join(folders + (key,)), "=", value, source)

    k.on_change(on_change)
```

Capabilities agents actually needed:

- Nested **get/set** with slash paths  
- **ensure_path** / mkdir (folderOrCreate-style)  
- **delete_key** for cleaning dashboards  
- **on_change** for live updates (config-val-updated / prop-saved)  
- Auth errors that point to **onboarding**, not stack traces alone  

Wire protocol matches the **Java SDK ReadyMode** (STOMP destinations, team topics). Java remains the product model for Spring apps; Python 0.2 is the **agent-native** twin.

### 2. MCP server (`kiponos-mcp`)

Long-lived stdio server. One connection per process. Tools:

| Tool | Role |
|------|------|
| `kiponos_onboarding` | Step list when tokens missing |
| `kiponos_status` | Connect + profile health |
| `kiponos_get` / `set` | Live read/write |
| `kiponos_list` / `dump` | Explore tree |
| `kiponos_mkdir` / `delete_key` | Structure |
| `kiponos_poll_events` | Drain value-change buffer |

Secrets stay in MCP `env` — not in chat, not in git.

### 3. Skill (`kiponos-live`)

Tells any agent:

1. Prefer MCP tools when present  
2. How to walk a human through https://kiponos.io Connect  
3. Never invent tokens; never pair profile A with tokens from app B  
4. Handshake 500 ≈ **mismatch**, not “wifi bad”

Install:

```bash
cd agent-kit
./install.sh          # pip + Grok/Cursor/Claude skill dirs
```

Grok MCP snippet:

```toml
[mcp_servers.kiponos]
command = "kiponos-mcp"
env = {
  KIPONOS_ID = "…",
  KIPONOS_ACCESS = "…",
  KIPONOS = "['my-app']['v1.0.0']['dev']['base']"
}
enabled = true
```

## Onboarding is product, not an afterthought

The first connection failures in our lab were not “SDK bugs.” They were **Unit-Tests tokens** still in the shell while the dashboard profile was **my-app / v1.0.0 / dev / base**. Same machine, wrong pair → HTTP 500 on WebSocket handshake.

So the kit teaches agents to **stop and onboard**:

1. Free account or login at [kiponos.io](https://kiponos.io)  
2. Connect Your App → app · release · env · config  
3. Copy **both** JWEs and the **exact** bracket profile  
4. Export env / MCP config  
5. `kiponos_status` until `ok: true`  

If you only ship an SDK, developers invent tokens. If you ship Skill + MCP with **`kiponos_onboarding`**, agents know to refuse and guide.

## Real-time collaboration that feels unfair

While organizing a `marketing/press/dev-to-crunch` folder we:

- Wrote queue and Crunchbase status from the publish pipeline  
- Split a fuzzy `waves-summary` into clear keys  
- Deleted legacy keys over the wire  

The human watched the dashboard: fields appeared and disappeared **as the agent worked** — no F5. That is not a demo gimmick. It is the same delta path a Spring Boot service uses for `failure_rate_threshold` during an incident.

For agents, that means:

- **Shared ops board** without Slack paste  
- **Human in the loop** who edits a value the agent can poll  
- **Status that does not lie** after a session crash (tree is on the server)

Control keys (pause waves, request sync, …) are the next layer — separate from this kit’s core CRUD. The important part is the **bus**.

## Implementation note — listener thread discipline

One bug taught production quality: if `on_change` runs **on the STOMP reader thread** and calls `set()` (which waits for ack), the client deadlocks. The MCP and any long-running handler must **queue** work to a worker thread. The Skill documents that; the MCP implements it for `poll_events` consumers.

```python
# Wrong: set() inside the listener callback
# Right: queue.put(event); worker thread calls set()
```

## Who this is for

- **Agent authors** who want live shared state without building a backend  
- **DevOps** who already use Kiponos for Java/Python services and want agents on the same tree  
- **Teams** tired of “restart the bot to pick up config”  

Java application integration remains the existing [Kiponos skill](https://github.com/kiponos-io/kiponos-io/tree/master/skills/kiponos) (`sdk-boot-3` on Maven Central). This Agent Kit is the **agent-facing** free bundle.

## Try it

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/agent-kit
./install.sh
export KIPONOS_ID=… KIPONOS_ACCESS=… KIPONOS="['my-app']['v1.0.0']['dev']['base']"
python3 -m kiponos.cli status
python3 -m kiponos.cli set demo/from-agent "hello-live"
```

Open the dashboard on that profile. No refresh.

## Closing

I reached for systemd because I know how to keep processes alive.  
The agent ecosystem already standardized that role: **MCP**.

The Skill tells the agent **how to behave**.  
The MCP **is** the always-connected peer.  
The SDK is the thin, honest client over a bus that was already live for production services.

That is the mind shift. Once you feel dashboard and agent update each other by delta, “config file + restart” feels like a previous decade — for humans **and** for agents.

---

**Links:** [Agent Kit](https://github.com/kiponos-io/kiponos-io/tree/master/agent-kit) · [Kiponos.io](https://kiponos.io) · [Getting started tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
