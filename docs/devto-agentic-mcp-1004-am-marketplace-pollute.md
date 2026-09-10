---
main_image: https://iili.io/nFFGv07.jpg
title: "Skills Context Pollution Is MCP Bloat With a Friendlier Name"
published: false
tags: ai, mcp, devops, java, kiponos
description: "Claude Code on mirror-phone wall: Macro-scale skill spam needs a live trust set. Dozens of low-quality skills consume budget before the user message"
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-agentic-mcp-1004-am-marketplace-pollute.md
---

I have sat next to the mirror-phone wall at 21:03 while Claude Code was *this close* to calling the wrong MCP tool.

Not a model failure. A **posture** failure.

Warned in 2026 skills-ecosystem writeups. Quality control cannot be 'hope the marketplace is nice'.

Someone on the floor said, out loud, **the skill is popular, leave it on**. Claude Code had started earlier. The tool list was frozen in `config.toml` / `.mcp.json` / a skill file. The only “safe” move anyone trusted was:

1. Kill Claude Code (or its MCP child)
2. Edit the server list
3. Restart the host
4. Lose the plan the model already paid for

That is the ceremony. **Dozens of low-quality skills consume budget before the user message**

**The Aha:** Popular is not trusted. `skill-trust` is the allow-list. Disk can keep the files. Real-time nested state is now a **primitive** — as native as a hashmap — via [Kiponos.io](https://kiponos.io). The host stays up. The leaf moves. The next tool call already obeys.

## The problem: Macro-scale skill spam needs a live trust set

Agentic programming in 2026 has a public fight: MCP vs CLI vs Skills. The likes on MCP and Grok Build posts are the tell. The industry is stuck on *how to expose tools*.

The missing primitive is **live nested state**:

| Where the roster hid | What you restart | What you lose |
|----------------------|------------------|---------------|
| MCP schema dump at session start | The host, sometimes the child `npx` | 16–72% of the window before the user speaks |
| `~/.grok/config.toml` / Cursor `mcp.json` | Claude Code | The open turn |
| SKILL.md / AGENTS.md | The next turn, sometimes the host | Context you already bought |
| “Just reconnect the server” | The MCP session | Tool-space you finally had right |

Dozens of low-quality skills consume budget before the user message

That is not a protocol bug you wait out. That is a process that cannot `get()` a tree that moved.

## What teams believe

| Belief | Production |
|--------|------------|
| A bigger context window fixes tool overload | 1M tokens still rot; selection still drops |
| Put the SDK in the SPA so the tile matches | Connect tokens do not belong in a browser |
| Feature flags cover MCP write | Flags are another product, another delay |
| Restart the agent host — it is cheap | Cheap until 21:03 ate the open turn |

## What is Kiponos here

Kiponos is a **live nested config hub**. Dashboard edit → WebSocket delta → in-memory tree on every connected process. Hot-path `get()` is a local map lookup. One WebSocket per process lifetime.

It is not an MCP server. It is not a Skill. It is the **control plane those things do not ship**: which tools may run, how fat a result may be, whether execute is allowed, whether a child inherits posture — *this turn*.

Profile shape: `['my-app']['v1.0.0']['dev']['base']`. The leaf in this story is `skill-trust` under `examples/agentic-mcp-1004-am-marketplace-pollute/skill-trust`.

## Architecture

![Architecture diagram](https://iili.io/nFL03u9.png)

The model still calls tools. The wrapper reads `skill-trust` **before** the call. No schema dump policy in argv.

## Config tree

```yaml
examples/agentic-mcp-1004-am-marketplace-pollute:
  skill-trust: core,reviewed
  schema-budget: "4000"
  result-cap: "2000"
  tool-search: "on"
  execute-gate: "plan"
```

Eight-plus keys when you pair `skill-trust` with sister dials (budget, mute, pause). They move together on the mirror-phone wall.

## Integration

Java tool wrapper (Claude Code just *calls* it):

```java
import io.kiponos.Kiponos;
import io.kiponos.Folder;

Folder gate = Kiponos.createForCurrentTeam()
    .getFolder("examples/agentic-mcp-1004-am-marketplace-pollute");
String roster = String.valueOf(gate.get("skill-trust"));
// Claude Code tool: refuse the dangerous MCP call when posture moved
```

Python peer:

```python
from kiponos import Kiponos

k = Kiponos.connect(quiet=True)
try:
    posture = k.get("examples/agentic-mcp-1004-am-marketplace-pollute/skill-trust", "live")
    if not str(posture):
        raise PermissionError("Macro-scale skill spam needs a live trust set gated live — host not restarted")
finally:
    k.disconnect()
```

React/Angular: **Node BFF** `@kiponos/react/server` / `@kiponos/angular/server`. SPA never holds Connect tokens.

Runnable mesh: `https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/agentic-mcp-1004-am-marketplace-pollute` — `./gradlew test run`.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| Three MCP servers at boot | 72% of the window gone | Live roster / schema-budget; dump shrinks |
| User says “no writes” mid-turn | Restart Claude Code | Set `skill-trust`; next call already denies |
| Second host started earlier | Paste the json into chat | Both `get()` the same leaf |
| Subagent spawned | Frozen argv copy | Child `get()`s `inherit-posture` |
| Tool returns 500k tokens | Context rot, doom loop | `result-cap` in the wrapper |

## Performance (this path)

- Wrapper `get()` is in-process after bootstrap.
- One WebSocket per Claude Code lifetime — not per MCP call.
- A dashboard edit is a **delta** of `skill-trust`, not a config.toml reload.
- You do not spend model tokens to “please restart Claude Code.”
- Schema tax is optional once the live set is small.

## Compare to alternatives

| Approach | Honest fit | Why it still restarts |
|----------|------------|------------------------|
| More MCP servers | More integrations | More schema, worse selection |
| Skills progressive disclosure | Good docs, lazy load | Not a multi-host bus |
| Claude Code tool search | Better client | Other hosts still dump |
| Redis poll in the tool | Shared, extra RTT | You invented a hub with worse UX |
| Feature-flag SaaS | Product experiments | Rarely session-safe for MCP |
| Kill -9 the npx child | Janitor | The turn still died |

## When not to use Kiponos

| Situation | Why |
|-----------|-----|
| Tool schema itself changed (new argument) | That *is* a code/Claude Code restart |
| Secret rotation of Connect / MCP OAuth tokens | Credentials are not live knobs |
| One-off local script, no peers | A hub is overkill |
| Browser-only “SDK in the SPA” | Forbidden — tokens leak or defaults lie |

## Pair `skill-trust` with a sister dial

`skill-trust` rarely moves alone on the mirror-phone wall. Pair it with `schema-budget`, `result-cap`, or `execute-gate` so you do not fix Macro-scale skill spam needs a live trust set by inventing a second incident.

## Why Claude Code is the wrong restart target

Claude Code is good at calling tools. It is not a control plane. Killing it to flip `skill-trust` teaches the on-call that judgment requires a process ID. The mirror-phone wall already disagrees.

## Getting started (15 minutes)

1. TeamPro on [kiponos.io](https://kiponos.io) → Connect → `KIPONOS_ID` / `KIPONOS_ACCESS` / profile `['my-app']['v1.0.0']['dev']['base']`.
2. Clone [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io).
3. `cd examples/java/agentic-mcp-1004-am-marketplace-pollute && cp kiponos.local.env.example kiponos.local.env`
4. `./gradlew test run` — prints `examples/agentic-mcp-1004-am-marketplace-pollute/skill-trust=...`
5. In the dashboard, change `skill-trust`. Keep Claude Code up. No MCP reconnect.
6. Point the tool wrapper at the same leaf. Do not ship a new server binary to flip Macro-scale skill spam needs a live trust set.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

## The moral

If flipping **Macro-scale skill spam needs a live trust set** requires restarting Claude Code or reconnecting MCP, you do not have agentic control. You have a hope with a process ID.

MCP, Skills, and tools are how agents *act*. **Kiponos is the live primitive they do not ship** — so the mirror-phone wall can change its mind without killing the session.

How to try: `examples/java/agentic-mcp-1004-am-marketplace-pollute` and `./gradlew test`.
