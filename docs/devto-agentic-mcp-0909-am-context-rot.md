---
main_image: https://iili.io/n2yIR9e.jpg
title: "Context Rot Hit at 25% of the Window and Grok Build Started Forgetting the Plan"
published: false
tags: ai, mcp, devops, java, kiponos
description: "Cursor on senses wall: Compact mode as a live leaf when the window starts eating itself. After ~25% of a 1M window, quality falls; long MCP sessions perform lik"
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-agentic-mcp-0909-am-context-rot.md
---

I have sat next to the senses wall at 14:12 while Cursor was *this close* to calling the wrong MCP tool.

Not a model failure. A **posture** failure.

Context Rot: past ~25% window use, frontier models degrade. MCP dumps make that happen before work starts.

Someone on the floor said, out loud, **the context is eating itself**. Cursor had started earlier. The tool list was frozen in `config.toml` / `.mcp.json` / a skill file. The only “safe” move anyone trusted was:

1. Kill Cursor (or its MCP child)
2. Edit the server list
3. Restart the host
4. Lose the plan the model already paid for

That is the ceremony. **After ~25% of a 1M window, quality falls; long MCP sessions perform like a worse model**

**The Aha:** Compact/defer is posture. Set `compact-mode=on` live. Do not kill the session to 'clear context'. Real-time nested state is now a **primitive** — as native as a hashmap — via [Kiponos.io](https://kiponos.io). The host stays up. The leaf moves. The next tool call already obeys.

## The problem: Compact mode as a live leaf when the window starts eating itself

Agentic programming in 2026 has a public fight: MCP vs CLI vs Skills. The likes on MCP and Grok Build posts are the tell. The industry is stuck on *how to expose tools*.

The missing primitive is **live nested state**:

| Where the roster hid | What you restart | What you lose |
|----------------------|------------------|---------------|
| MCP schema dump at session start | The host, sometimes the child `npx` | 16–72% of the window before the user speaks |
| `~/.grok/config.toml` / Cursor `mcp.json` | Cursor | The open turn |
| SKILL.md / AGENTS.md | The next turn, sometimes the host | Context you already bought |
| “Just reconnect the server” | The MCP session | Tool-space you finally had right |

After ~25% of a 1M window, quality falls; long MCP sessions perform like a worse model

That is not a protocol bug you wait out. That is a process that cannot `get()` a tree that moved.

## What teams believe

| Belief | Production |
|--------|------------|
| A bigger context window fixes tool overload | 1M tokens still rot; selection still drops |
| Put the SDK in the SPA so the tile matches | Connect tokens do not belong in a browser |
| Feature flags cover MCP write | Flags are another product, another delay |
| Restart the agent host — it is cheap | Cheap until 14:12 ate the open turn |

## What is Kiponos here

Kiponos is a **live nested config hub**. Dashboard edit → WebSocket delta → in-memory tree on every connected process. Hot-path `get()` is a local map lookup. One WebSocket per process lifetime.

It is not an MCP server. It is not a Skill. It is the **control plane those things do not ship**: which tools may run, how fat a result may be, whether execute is allowed, whether a child inherits posture — *this turn*.

Profile shape: `['my-app']['v1.0.0']['dev']['base']`. The leaf in this story is `compact-mode` under `examples/agentic-mcp-0909-am-context-rot/compact-mode`.

## Architecture

![Architecture diagram](https://iili.io/n3dGEtj.png)

The model still calls tools. The wrapper reads `compact-mode` **before** the call. No schema dump policy in argv.

## Config tree

```yaml
examples/agentic-mcp-0909-am-context-rot:
  compact-mode: off
  schema-budget: "4000"
  result-cap: "2000"
  tool-search: "on"
  execute-gate: "plan"
```

Eight-plus keys when you pair `compact-mode` with sister dials (budget, mute, pause). They move together on the senses wall.

## Integration

Java tool wrapper (Cursor just *calls* it):

```java
import io.kiponos.Kiponos;
import io.kiponos.Folder;

Folder gate = Kiponos.createForCurrentTeam()
    .getFolder("examples/agentic-mcp-0909-am-context-rot");
String roster = String.valueOf(gate.get("compact-mode"));
// Cursor tool: refuse the dangerous MCP call when posture moved
```

Python peer:

```python
from kiponos import Kiponos

k = Kiponos.connect(quiet=True)
try:
    posture = k.get("examples/agentic-mcp-0909-am-context-rot/compact-mode", "live")
    if not str(posture):
        raise PermissionError("Compact mode as a live leaf when the window starts eating itself gated live — host not restarted")
finally:
    k.disconnect()
```

React/Angular: **Node BFF** `@kiponos/react/server` / `@kiponos/angular/server`. SPA never holds Connect tokens.

Runnable mesh: `https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/agentic-mcp-0909-am-context-rot` — `./gradlew test run`.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| Three MCP servers at boot | 72% of the window gone | Live roster / schema-budget; dump shrinks |
| User says “no writes” mid-turn | Restart Cursor | Set `compact-mode`; next call already denies |
| Second host started earlier | Paste the json into chat | Both `get()` the same leaf |
| Subagent spawned | Frozen argv copy | Child `get()`s `inherit-posture` |
| Tool returns 500k tokens | Context rot, doom loop | `result-cap` in the wrapper |

## Performance (this path)

- Wrapper `get()` is in-process after bootstrap.
- One WebSocket per Cursor lifetime — not per MCP call.
- A dashboard edit is a **delta** of `compact-mode`, not a config.toml reload.
- You do not spend model tokens to “please restart Cursor.”
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
| Tool schema itself changed (new argument) | That *is* a code/Cursor restart |
| Secret rotation of Connect / MCP OAuth tokens | Credentials are not live knobs |
| One-off local script, no peers | A hub is overkill |
| Browser-only “SDK in the SPA” | Forbidden — tokens leak or defaults lie |

## Pair `compact-mode` with a sister dial

`compact-mode` rarely moves alone on the senses wall. Pair it with `schema-budget`, `result-cap`, or `execute-gate` so you do not fix Compact mode as a live leaf when the window starts eating itself by inventing a second incident.

## Why Cursor is the wrong restart target

Cursor is good at calling tools. It is not a control plane. Killing it to flip `compact-mode` teaches the on-call that judgment requires a process ID. The senses wall already disagrees.

## Getting started (15 minutes)

1. TeamPro on [kiponos.io](https://kiponos.io) → Connect → `KIPONOS_ID` / `KIPONOS_ACCESS` / profile `['my-app']['v1.0.0']['dev']['base']`.
2. Clone [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io).
3. `cd examples/java/agentic-mcp-0909-am-context-rot && cp kiponos.local.env.example kiponos.local.env`
4. `./gradlew test run` — prints `examples/agentic-mcp-0909-am-context-rot/compact-mode=...`
5. In the dashboard, change `compact-mode`. Keep Cursor up. No MCP reconnect.
6. Point the tool wrapper at the same leaf. Do not ship a new server binary to flip Compact mode as a live leaf when the window starts eating itself.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

## The moral

If flipping **Compact mode as a live leaf when the window starts eating itself** requires restarting Cursor or reconnecting MCP, you do not have agentic control. You have a hope with a process ID.

MCP, Skills, and tools are how agents *act*. **Kiponos is the live primitive they do not ship** — so the senses wall can change its mind without killing the session.

How to try: `examples/java/agentic-mcp-0909-am-context-rot` and `./gradlew test`.
