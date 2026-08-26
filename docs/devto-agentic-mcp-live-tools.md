---
main_image: https://iili.io/Cb53K8P.jpg
title: "Grok Build Still Restarted MCP to Flip a Write Tool. The Shopping Admin Did Not Need That."
published: false
tags: python, java, mcp, devops, ai
description: Agentic hosts restart MCP servers to change a tool allow-list. Kiponos SDK makes that list a live hub leaf — Grok Build, Cursor, and Claude Code keep the session; the next tool call already sees the gate.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-agentic-mcp-live-tools.md
---

I have sat in a shopping-admin war room where the agent was *this close* to issuing a stock write during a flash freeze.

Not a model failure. A **tool** failure.

The MCP host had started with `tools-allow=search,read,write`. Someone on the floor said, out loud, **disable write**. The Cursor session, the Claude Code tab, and the Grok Build run were all still holding the old process. The only “safe” move anyone trusted was:

1. Kill the MCP server  
2. Edit an env file  
3. Restart the host  
4. Lose the thread, the tool context, and ten minutes of shopping-cart forensics  

I have watched that restart more times than I want to admit. It feels responsible. It is a ceremony. The cart freeze does not wait for ceremonies.

**The Aha:** an MCP tool is not a binary you reboot. It is a **function that should read live posture** from [Kiponos.io](https://kiponos.io) on every call. The host stays up. The gate moves.

## The problem: the allow-list lived in the process, not in the turn

Grok Build, Cursor, and Claude Code are good at *calling* tools. They are not born with a **shared, instant, restart-free control plane**.

So teams hide policy in the only places agent frameworks actually ship:

| Where the gate hid | What you restart | What you lose |
|--------------------|------------------|---------------|
| MCP server env / argv | The MCP process | Open tool sessions |
| Skill file on disk | The agent turn, sometimes the host | Context the model already paid for |
| `.cursor` / local JSON | Whatever still has the file open | Agreement between two agents |
| Hard-coded `if tool == write` | A release | The incident clock |

The shopping admin dashboard already *knew* writes were illegal. The travel-coordinator group chat already *knew* the noisy channel should be read-only. The agent host did not, because it had started earlier.

That is the missing piece: **the framework gave you tools. It did not give you a live hub.**

## What teams believe

| Belief | Production |
|--------|------------|
| “We’ll just restart MCP, it’s cheap” | Cheap until the agent was mid-diagnosis on a live cart |
| “The skill file is the source of truth” | Skills are instructions. They are not a fan-out bus |
| “Put the SDK in the admin SPA” | Connect tokens do not belong in a browser |
| “Feature flags cover tool gates” | Flags are another product, another delay, another outage mode |

## The Aha: local get, live write, host stays up

Kiponos holds a nested tree. Java and Python SDKs keep the latest values **in memory**, patched over WebSocket deltas. The hot path inside an MCP tool is a **local get** — no HTTP RTT per cart lookup.

Hub leaf for this essay:

```text
examples/agentic-mcp-live-tools/tools-allow = search,read
```

Runnable proof: [`examples/java/agentic-mcp-live-tools`](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/agentic-mcp-live-tools)

![Architecture diagram](https://iili.io/Cb53SKQ.png)

## Config tree (shopping admin + travel coordinator)

```yaml
examples/
  agentic-mcp-live-tools/
    tools-allow: search,read          # write stripped during freeze
    write-reason: flash-freeze
apps/
  shopping/
    admin/
      inventory-writes: paused
  travel/
    coordinator/
      group-chat-writes: muted
```

Public SDKs: **Java**, **Python**, plus React/Angular **server** peers (`createFromEnv`). Never put Connect tokens in the SPA.

## Integration — Java hot path

```java
Kiponos kip = Kiponos.createForCurrentTeam();
Folder gate = kip.getRootFolder()
        .folderOrCreate("examples")
        .folderOrCreate("agentic-mcp-live-tools");
if (!gate.hasKey("tools-allow")) {
    gate.set("tools-allow", "search,read");
}

boolean canWrite = Arrays.stream(
        gate.get("tools-allow").split(","))
        .map(String::trim)
        .anyMatch("write"::equals);
// shopping admin MCP: refuse stock mutation when write is gone
```

Same leaf from a Python MCP tool (Grok Build, Cursor, Claude Code all just *call* the tool):

```python
from kiponos import Kiponos

k = Kiponos.connect(quiet=True)  # env: KIPONOS_ID, KIPONOS_ACCESS, KIPONOS
try:
    raw = k.get("examples/agentic-mcp-live-tools/tools-allow", "search,read")
    allow = {p.strip() for p in str(raw).split(",")}
    if "write" not in allow:
        raise PermissionError("write gated live — host not restarted")
finally:
    k.disconnect()
```

The MCP **process** does not recycle. The **next** `tools/call` already sees the dashboard edit.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| Shopping flash freeze | Restart MCP; agent forgets the SKU thread | Set `tools-allow=search,read`; next call denies write |
| Travel group chat spam | Kill the host to drop the send tool | Mute writes on the noisy channel leaf |
| Two agents, one policy | Paste the new list into both chats | One hub leaf; both MCP tools `get()` locally |
| Admin dashboard shows “paused” | Agent still writes because it started earlier | Dashboard and tool share the same memory tree |
| Incident over, resume writes | Another restart | Set `write` back; session continues |

## Performance (this path, not a generic table)

- MCP tool `get()` is an in-process map lookup after bootstrap.
- One WebSocket per process lifetime — not per cart line.
- A dashboard edit is a **delta** of one leaf, not a config-file reload.
- You do not pay model tokens to “please restart your tools.”
- Two agent hosts (Cursor + Claude Code) converge without a third paste.

## Compare to alternatives

| Approach | Honest fit | Why it still restarts |
|----------|------------|------------------------|
| Env file + MCP reboot | Simple at 09:00 | The freeze is at 21:14 |
| Skill markdown as policy | Good instructions | Not a live bus |
| Redis poll inside the tool | Shared, but RTT on the hot path | You invented a hub with worse UX |
| Feature-flag SaaS | Product experiments | Rarely MCP-session-safe |
| `@RefreshScope` / actuator | JVM apps | Does not restart Cursor |

## When not to use Kiponos

| Situation | Why |
|-----------|-----|
| Tool schema itself changed (new argument) | That *is* a code/MCP restart |
| Secret rotation of Connect tokens | Credentials are not live knobs |
| One-off local script, no peers | A hub is overkill |
| Browser-only “SDK in the SPA” | Forbidden — tokens leak or defaults lie |

## Getting started (15 minutes)

1. TeamPro on [kiponos.io](https://kiponos.io) → Connect → `KIPONOS_ID` / `KIPONOS_ACCESS` / profile `['my-app']['v1.0.0']['dev']['base']`.
2. Clone [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io).
3. `cd examples/java/agentic-mcp-live-tools && cp kiponos.local.env.example kiponos.local.env`
4. `./gradlew test run` — prints `examples/agentic-mcp-live-tools/tools-allow=...`
5. In the dashboard, strip `write`. Run again (or keep the process up and `get()` in a loop). No rebuild.
6. Point your MCP tool at the same leaf. Grok Build / Cursor / Claude Code do not need a new server binary for a gate flip.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

## The moral

If flipping a write tool requires restarting Grok Build, Cursor, or Claude Code, you do not have a gate. You have a hope with a process ID.

Agent frameworks already know how to call MCP. **Kiponos is the live hub they do not ship** — so shopping admin and travel coordinator tools can change their minds without killing the session.

How to try: `examples/java/agentic-mcp-live-tools` and `./gradlew test`.
