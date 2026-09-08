---
main_image: https://iili.io/n3uEd12.jpg
title: "MCP Log Spam Flooded the Turn. Muting Required Killing the Server"
published: false
tags: ai, mcp, devops, java, kiponos
description: "Cursor on senses wall: Mute noisy tools live, keep the session. Verbose tool results and logs steal the window; 'quiet mode' is a restart flag"
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-agentic-mcp-0921-am-tools-mute.md
---

I have sat next to the senses wall at 00:40 while Cursor was *this close* to calling the wrong MCP tool.

Not a model failure. A **posture** failure.

Tool-result noise is a named production failure class. Projection belongs in the tool, gated live.

Someone on the floor said, out loud, **set LOG_LEVEL=error and bounce npx**. Cursor had started earlier. The tool list was frozen in `config.toml` / `.mcp.json` / a skill file. The only “safe” move anyone trusted was:

1. Kill Cursor (or its MCP child)
2. Edit the server list
3. Restart the host
4. Lose the plan the model already paid for

That is the ceremony. **Verbose tool results and logs steal the window; 'quiet mode' is a restart flag**

**The Aha:** `tools-mute=github` live. Results stay tiny. Grok Build keeps its plan. Real-time nested state is now a **primitive** — as native as a hashmap — via [Kiponos.io](https://kiponos.io). The host stays up. The leaf moves. The next tool call already obeys.

## The problem: Mute noisy tools live, keep the session

Agentic programming in 2026 has a public fight: MCP vs CLI vs Skills. The likes on MCP and Grok Build posts are the tell. The industry is stuck on *how to expose tools*.

The missing primitive is **live nested state**:

| Where the roster hid | What you restart | What you lose |
|----------------------|------------------|---------------|
| MCP schema dump at session start | The host, sometimes the child `npx` | 16–72% of the window before the user speaks |
| `~/.grok/config.toml` / Cursor `mcp.json` | Cursor | The open turn |
| SKILL.md / AGENTS.md | The next turn, sometimes the host | Context you already bought |
| “Just reconnect the server” | The MCP session | Tool-space you finally had right |

Verbose tool results and logs steal the window; 'quiet mode' is a restart flag

That is not a protocol bug you wait out. That is a process that cannot `get()` a tree that moved.

## What teams believe

| Belief | Production |
|--------|------------|
| Put the SDK in the SPA so the tile matches | Connect tokens do not belong in a browser |
| Feature flags cover MCP write | Flags are another product, another delay |
| Restart the agent host — it is cheap | Cheap until 00:40 ate the open turn |
| MCP is the standard so we are done | MCP is how you call. It is not live posture |

## What is Kiponos here

Kiponos is a **live nested config hub**. Dashboard edit → WebSocket delta → in-memory tree on every connected process. Hot-path `get()` is a local map lookup. One WebSocket per process lifetime.

It is not an MCP server. It is not a Skill. It is the **control plane those things do not ship**: which tools may run, how fat a result may be, whether execute is allowed, whether a child inherits posture — *this turn*.

Profile shape: `['my-app']['v1.0.0']['dev']['base']`. The leaf in this story is `chat-mute` under `examples/agentic-mcp-0921-am-tools-mute/tools-mute`.

## Architecture

![Architecture diagram](https://iili.io/n3mcsGp.png)

The model still calls tools. The wrapper reads `chat-mute` **before** the call. No schema dump policy in argv.

## Config tree

```yaml
examples/agentic-mcp-0921-am-tools-mute:
  chat-mute: none
  schema-budget: "4000"
  result-cap: "2000"
  tool-search: "on"
  execute-gate: "plan"
```

Eight-plus keys when you pair `chat-mute` with sister dials (budget, mute, pause). They move together on the senses wall.

## Integration

Java tool wrapper (Cursor just *calls* it):

```java
import io.kiponos.Kiponos;
import io.kiponos.Folder;

Folder gate = Kiponos.createForCurrentTeam()
    .getFolder("examples/agentic-mcp-0921-am-tools-mute");
String roster = String.valueOf(gate.get("chat-mute"));
// Cursor tool: refuse the dangerous MCP call when posture moved
```

Python peer:

```python
from kiponos import Kiponos

k = Kiponos.connect(quiet=True)
try:
    posture = k.get("examples/agentic-mcp-0921-am-tools-mute/tools-mute", "live")
    if not str(posture):
        raise PermissionError("Mute noisy tools live, keep the session gated live — host not restarted")
finally:
    k.disconnect()
```

React/Angular: **Node BFF** `@kiponos/react/server` / `@kiponos/angular/server`. SPA never holds Connect tokens.

Runnable mesh: `https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/agentic-mcp-0921-am-tools-mute` — `./gradlew test run`.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| Three MCP servers at boot | 72% of the window gone | Live roster / schema-budget; dump shrinks |
| User says “no writes” mid-turn | Restart Cursor | Set `chat-mute`; next call already denies |
| Second host started earlier | Paste the json into chat | Both `get()` the same leaf |
| Subagent spawned | Frozen argv copy | Child `get()`s `inherit-posture` |
| Tool returns 500k tokens | Context rot, doom loop | `result-cap` in the wrapper |

## Performance (this path)

- Wrapper `get()` is in-process after bootstrap.
- One WebSocket per Cursor lifetime — not per MCP call.
- A dashboard edit is a **delta** of `chat-mute`, not a config.toml reload.
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

## Pair `chat-mute` with a sister dial

`chat-mute` rarely moves alone on the senses wall. Pair it with `schema-budget`, `result-cap`, or `execute-gate` so you do not fix Mute noisy tools live, keep the session by inventing a second incident.

## Why Cursor is the wrong restart target

Cursor is good at calling tools. It is not a control plane. Killing it to flip `chat-mute` teaches the on-call that judgment requires a process ID. The senses wall already disagrees.

## Getting started (15 minutes)

1. TeamPro on [kiponos.io](https://kiponos.io) → Connect → `KIPONOS_ID` / `KIPONOS_ACCESS` / profile `['my-app']['v1.0.0']['dev']['base']`.
2. Clone [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io).
3. `cd examples/java/agentic-mcp-0921-am-tools-mute && cp kiponos.local.env.example kiponos.local.env`
4. `./gradlew test run` — prints `examples/agentic-mcp-0921-am-tools-mute/tools-mute=...`
5. In the dashboard, change `chat-mute`. Keep Cursor up. No MCP reconnect.
6. Point the tool wrapper at the same leaf. Do not ship a new server binary to flip Mute noisy tools live, keep the session.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

## The moral

If flipping **Mute noisy tools live, keep the session** requires restarting Cursor or reconnecting MCP, you do not have agentic control. You have a hope with a process ID.

MCP, Skills, and tools are how agents *act*. **Kiponos is the live primitive they do not ship** — so the senses wall can change its mind without killing the session.

How to try: `examples/java/agentic-mcp-0921-am-tools-mute` and `./gradlew test`.
