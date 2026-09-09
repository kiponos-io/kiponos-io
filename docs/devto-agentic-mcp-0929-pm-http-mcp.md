---
main_image: https://iili.io/nFFEbt4.jpg
title: "Remote HTTP MCP Authenticated Fine. I Still Restarted to Hide One Tool"
published: false
tags: ai, mcp, devops, java, kiponos
description: "Claude Code on shopping-admin wall: Remote vs stdio does not remove the roster problem. OAuth succeeded. The tool list is still all-or-nothing per connection."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-agentic-mcp-0929-pm-http-mcp.md
---

I have sat next to the shopping-admin wall at 18:45 while Claude Code was *this close* to calling the wrong MCP tool.

Not a model failure. A **posture** failure.

Grok `mcp add --transport http`. Auth is solved-ish. Live subset is not.

Someone on the floor said, out loud, **reconnect the Linear HTTP MCP without issues.write**. Claude Code had started earlier. The tool list was frozen in `config.toml` / `.mcp.json` / a skill file. The only “safe” move anyone trusted was:

1. Kill Claude Code (or its MCP child)
2. Edit the server list
3. Restart the host
4. Lose the plan the model already paid for

That is the ceremony. **OAuth succeeded. The tool list is still all-or-nothing per connection.**

**The Aha:** Connection stays. `tools-allow` drops write. Next call already 403s in *your* wrapper. Real-time nested state is now a **primitive** — as native as a hashmap — via [Kiponos.io](https://kiponos.io). The host stays up. The leaf moves. The next tool call already obeys.

## The problem: Remote vs stdio does not remove the roster problem

Agentic programming in 2026 has a public fight: MCP vs CLI vs Skills. The likes on MCP and Grok Build posts are the tell. The industry is stuck on *how to expose tools*.

The missing primitive is **live nested state**:

| Where the roster hid | What you restart | What you lose |
|----------------------|------------------|---------------|
| MCP schema dump at session start | The host, sometimes the child `npx` | 16–72% of the window before the user speaks |
| `~/.grok/config.toml` / Cursor `mcp.json` | Claude Code | The open turn |
| SKILL.md / AGENTS.md | The next turn, sometimes the host | Context you already bought |
| “Just reconnect the server” | The MCP session | Tool-space you finally had right |

OAuth succeeded. The tool list is still all-or-nothing per connection.

That is not a protocol bug you wait out. That is a process that cannot `get()` a tree that moved.

## What teams believe

| Belief | Production |
|--------|------------|
| Restart the agent host — it is cheap | Cheap until 18:45 ate the open turn |
| MCP is the standard so we are done | MCP is how you call. It is not live posture |
| Skills will progressive-disclose it | Skills are files. Files do not fan out |
| A bigger context window fixes tool overload | 1M tokens still rot; selection still drops |

## What is Kiponos here

Kiponos is a **live nested config hub**. Dashboard edit → WebSocket delta → in-memory tree on every connected process. Hot-path `get()` is a local map lookup. One WebSocket per process lifetime.

It is not an MCP server. It is not a Skill. It is the **control plane those things do not ship**: which tools may run, how fat a result may be, whether execute is allowed, whether a child inherits posture — *this turn*.

Profile shape: `['my-app']['v1.0.0']['dev']['base']`. The leaf in this story is `tools-allow` under `examples/agentic-mcp-0929-pm-http-mcp/tools-allow`.

## Architecture

```mermaid
flowchart LR
  wall[shopping-admin wall] -->|set tools-allow| hub[Kiponos hub]
  hub -->|delta WS| gb[Claude Code process]
  gb -->|get tools-allow| tool[MCP / Skill / tool wrapper]
  tool -->|allow or deny| world[External system]
```

The model still calls tools. The wrapper reads `tools-allow` **before** the call. No schema dump policy in argv.

## Config tree

```yaml
examples/agentic-mcp-0929-pm-http-mcp:
  tools-allow: search,read
  schema-budget: "4000"
  result-cap: "2000"
  tool-search: "on"
  execute-gate: "plan"
```

Eight-plus keys when you pair `tools-allow` with sister dials (budget, mute, pause). They move together on the shopping-admin wall.

## Integration

Java tool wrapper (Claude Code just *calls* it):

```java
import io.kiponos.Kiponos;
import io.kiponos.Folder;

Folder gate = Kiponos.createForCurrentTeam()
    .getFolder("examples/agentic-mcp-0929-pm-http-mcp");
String roster = String.valueOf(gate.get("tools-allow"));
// Claude Code tool: refuse the dangerous MCP call when posture moved
```

Python peer:

```python
from kiponos import Kiponos

k = Kiponos.connect(quiet=True)
try:
    posture = k.get("examples/agentic-mcp-0929-pm-http-mcp/tools-allow", "live")
    if not str(posture):
        raise PermissionError("Remote vs stdio does not remove the roster problem gated live — host not restarted")
finally:
    k.disconnect()
```

React/Angular: **Node BFF** `@kiponos/react/server` / `@kiponos/angular/server`. SPA never holds Connect tokens.

Runnable mesh: `https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/agentic-mcp-0929-pm-http-mcp` — `./gradlew test run`.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| Three MCP servers at boot | 72% of the window gone | Live roster / schema-budget; dump shrinks |
| User says “no writes” mid-turn | Restart Claude Code | Set `tools-allow`; next call already denies |
| Second host started earlier | Paste the json into chat | Both `get()` the same leaf |
| Subagent spawned | Frozen argv copy | Child `get()`s `inherit-posture` |
| Tool returns 500k tokens | Context rot, doom loop | `result-cap` in the wrapper |

## Performance (this path)

- Wrapper `get()` is in-process after bootstrap.
- One WebSocket per Claude Code lifetime — not per MCP call.
- A dashboard edit is a **delta** of `tools-allow`, not a config.toml reload.
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

## Pair `tools-allow` with a sister dial

`tools-allow` rarely moves alone on the shopping-admin wall. Pair it with `schema-budget`, `result-cap`, or `execute-gate` so you do not fix Remote vs stdio does not remove the roster problem by inventing a second incident.

## Why Claude Code is the wrong restart target

Claude Code is good at calling tools. It is not a control plane. Killing it to flip `tools-allow` teaches the on-call that judgment requires a process ID. The shopping-admin wall already disagrees.

## Getting started (15 minutes)

1. TeamPro on [kiponos.io](https://kiponos.io) → Connect → `KIPONOS_ID` / `KIPONOS_ACCESS` / profile `['my-app']['v1.0.0']['dev']['base']`.
2. Clone [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io).
3. `cd examples/java/agentic-mcp-0929-pm-http-mcp && cp kiponos.local.env.example kiponos.local.env`
4. `./gradlew test run` — prints `examples/agentic-mcp-0929-pm-http-mcp/tools-allow=...`
5. In the dashboard, change `tools-allow`. Keep Claude Code up. No MCP reconnect.
6. Point the tool wrapper at the same leaf. Do not ship a new server binary to flip Remote vs stdio does not remove the roster problem.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

## The moral

If flipping **Remote vs stdio does not remove the roster problem** requires restarting Claude Code or reconnecting MCP, you do not have agentic control. You have a hope with a process ID.

MCP, Skills, and tools are how agents *act*. **Kiponos is the live primitive they do not ship** — so the shopping-admin wall can change its mind without killing the session.

How to try: `examples/java/agentic-mcp-0929-pm-http-mcp` and `./gradlew test`.
