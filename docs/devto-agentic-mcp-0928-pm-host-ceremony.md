---
main_image: https://iili.io/nFFEiFt.jpg
title: "The Ceremony Is Always the Same: Kill Host, Edit JSON, Restart, Lose the Turn"
published: false
tags: ai, mcp, devops, java, kiponos
description: "Cursor on admin-dashboard wall: Name the ceremony so people can hate it. Four steps everyone trusts because it feels responsible"
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-agentic-mcp-0928-pm-host-ceremony.md
---

I have sat next to the admin-dashboard wall at 22:19 while Cursor was *this close* to calling the wrong MCP tool.

Not a model failure. A **posture** failure.

This is the Kiponos traveler voice applied to MCP. The likes say the audience is here.

Someone on the floor said, out loud, **the safe move is restart**. Cursor had started earlier. The tool list was frozen in `config.toml` / `.mcp.json` / a skill file. The only “safe” move anyone trusted was:

1. Kill Cursor (or its MCP child)
2. Edit the server list
3. Restart the host
4. Lose the plan the model already paid for

That is the ceremony. **Four steps everyone trusts because it feels responsible**

**The Aha:** Safe is the next `get()`. Ceremony is how you lose the plan you already paid for. Real-time nested state is now a **primitive** — as native as a hashmap — via [Kiponos.io](https://kiponos.io). The host stays up. The leaf moves. The next tool call already obeys.

## The problem: Name the ceremony so people can hate it

Agentic programming in 2026 has a public fight: MCP vs CLI vs Skills. The likes on MCP and Grok Build posts are the tell. The industry is stuck on *how to expose tools*.

The missing primitive is **live nested state**:

| Where the roster hid | What you restart | What you lose |
|----------------------|------------------|---------------|
| MCP schema dump at session start | The host, sometimes the child `npx` | 16–72% of the window before the user speaks |
| `~/.grok/config.toml` / Cursor `mcp.json` | Cursor | The open turn |
| SKILL.md / AGENTS.md | The next turn, sometimes the host | Context you already bought |
| “Just reconnect the server” | The MCP session | Tool-space you finally had right |

Four steps everyone trusts because it feels responsible

That is not a protocol bug you wait out. That is a process that cannot `get()` a tree that moved.

## What teams believe

| Belief | Production |
|--------|------------|
| MCP is the standard so we are done | MCP is how you call. It is not live posture |
| Skills will progressive-disclose it | Skills are files. Files do not fan out |
| A bigger context window fixes tool overload | 1M tokens still rot; selection still drops |
| Put the SDK in the SPA so the tile matches | Connect tokens do not belong in a browser |

## What is Kiponos here

Kiponos is a **live nested config hub**. Dashboard edit → WebSocket delta → in-memory tree on every connected process. Hot-path `get()` is a local map lookup. One WebSocket per process lifetime.

It is not an MCP server. It is not a Skill. It is the **control plane those things do not ship**: which tools may run, how fat a result may be, whether execute is allowed, whether a child inherits posture — *this turn*.

Profile shape: `['my-app']['v1.0.0']['dev']['base']`. The leaf in this story is `tools-allow` under `examples/agentic-mcp-0928-pm-host-ceremony/tools-allow`.

## Architecture

![Architecture diagram](https://iili.io/nFq7kPt.png)

The model still calls tools. The wrapper reads `tools-allow` **before** the call. No schema dump policy in argv.

## Config tree

```yaml
examples/agentic-mcp-0928-pm-host-ceremony:
  tools-allow: search,read
  schema-budget: "4000"
  result-cap: "2000"
  tool-search: "on"
  execute-gate: "plan"
```

Eight-plus keys when you pair `tools-allow` with sister dials (budget, mute, pause). They move together on the admin-dashboard wall.

## Integration

Java tool wrapper (Cursor just *calls* it):

```java
import io.kiponos.Kiponos;
import io.kiponos.Folder;

Folder gate = Kiponos.createForCurrentTeam()
    .getFolder("examples/agentic-mcp-0928-pm-host-ceremony");
String roster = String.valueOf(gate.get("tools-allow"));
// Cursor tool: refuse the dangerous MCP call when posture moved
```

Python peer:

```python
from kiponos import Kiponos

k = Kiponos.connect(quiet=True)
try:
    posture = k.get("examples/agentic-mcp-0928-pm-host-ceremony/tools-allow", "live")
    if not str(posture):
        raise PermissionError("Name the ceremony so people can hate it gated live — host not restarted")
finally:
    k.disconnect()
```

React/Angular: **Node BFF** `@kiponos/react/server` / `@kiponos/angular/server`. SPA never holds Connect tokens.

Runnable mesh: `https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/agentic-mcp-0928-pm-host-ceremony` — `./gradlew test run`.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| Three MCP servers at boot | 72% of the window gone | Live roster / schema-budget; dump shrinks |
| User says “no writes” mid-turn | Restart Cursor | Set `tools-allow`; next call already denies |
| Second host started earlier | Paste the json into chat | Both `get()` the same leaf |
| Subagent spawned | Frozen argv copy | Child `get()`s `inherit-posture` |
| Tool returns 500k tokens | Context rot, doom loop | `result-cap` in the wrapper |

## Performance (this path)

- Wrapper `get()` is in-process after bootstrap.
- One WebSocket per Cursor lifetime — not per MCP call.
- A dashboard edit is a **delta** of `tools-allow`, not a config.toml reload.
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

## Pair `tools-allow` with a sister dial

`tools-allow` rarely moves alone on the admin-dashboard wall. Pair it with `schema-budget`, `result-cap`, or `execute-gate` so you do not fix Name the ceremony so people can hate it by inventing a second incident.

## Why Cursor is the wrong restart target

Cursor is good at calling tools. It is not a control plane. Killing it to flip `tools-allow` teaches the on-call that judgment requires a process ID. The admin-dashboard wall already disagrees.

## Getting started (15 minutes)

1. TeamPro on [kiponos.io](https://kiponos.io) → Connect → `KIPONOS_ID` / `KIPONOS_ACCESS` / profile `['my-app']['v1.0.0']['dev']['base']`.
2. Clone [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io).
3. `cd examples/java/agentic-mcp-0928-pm-host-ceremony && cp kiponos.local.env.example kiponos.local.env`
4. `./gradlew test run` — prints `examples/agentic-mcp-0928-pm-host-ceremony/tools-allow=...`
5. In the dashboard, change `tools-allow`. Keep Cursor up. No MCP reconnect.
6. Point the tool wrapper at the same leaf. Do not ship a new server binary to flip Name the ceremony so people can hate it.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

## The moral

If flipping **Name the ceremony so people can hate it** requires restarting Cursor or reconnecting MCP, you do not have agentic control. You have a hope with a process ID.

MCP, Skills, and tools are how agents *act*. **Kiponos is the live primitive they do not ship** — so the admin-dashboard wall can change its mind without killing the session.

How to try: `examples/java/agentic-mcp-0928-pm-host-ceremony` and `./gradlew test`.
