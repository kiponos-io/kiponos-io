---
main_image: https://iili.io/CyEUWKX.jpg
title: "MCP host Finished the Turn Blind — Agents choose a metadata folder on the Admin Dashboard Path"
published: false
tags: java, python, devops, ai, kiponos
description: "MCP host kept finishing the turn blind on admin-dashboard wall. Agents choose a metadata folder is a live hub leaf."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-agentic-dev-0924-pm-agent-folder.md
---

I have sat next to the admin-dashboard wall at 21:03 while MCP host was *this close* to doing the wrong thing.

Not a model failure. A **posture** failure.

The host had started with `owner-agent` frozen in env / argv / a skill file. Someone on the floor said, out loud, **the tile is not a rumor. it is the same tree**. MCP host was still holding the old process. The only “safe” move anyone trusted was:

1. Kill MCP host (or its MCP server)
2. Edit a file
3. Restart the host
4. Lose the operator's last ten minutes of diagnosis

I have watched that restart more times than I want to admit. It feels responsible. It is a ceremony. **status tile green while the agent host started earlier** does not wait for ceremonies.

**The Aha:** Agents choose a metadata folder is not a binary you reboot. It is a **function that should read live posture** from [Kiponos.io](https://kiponos.io) on every call. The host stays up. The leaf moves.

## The problem: Agents choose a metadata folder lived in the process, not in the turn

MCP host is good at *calling* tools. It is not born with a **shared, instant, restart-free control plane**.

So teams hide Agents choose a metadata folder in the only places agent frameworks actually ship:

| Where the gate hid | What you restart | What you lose |
|--------------------|------------------|---------------|
| MCP server env / argv | The MCP process | Open tool sessions |
| Skill file on disk | The agent turn, sometimes the host | Context the model already paid for |
| Host-local JSON | Whatever still has the file open | Agreement between two agents |
| Hard-coded `if` on `owner-agent` | A release | The incident clock |

The admin-dashboard wall already *knew*. MCP host did not, because it had started earlier.

That is the missing piece: **the framework gave you tools. It did not give you a live hub.**

## What teams believe

| Belief | Production |
|--------|------------|
| We'll catch it next turn | The admin-dashboard wall already knew this turn |
| Restart MCP host — it is cheap | Cheap until 21:03 ate the operator's last ten minutes of diagnosis |
| The skill file is the source of truth | Skills instruct. They do not fan out |
| Put the SDK in the SPA | Connect tokens do not belong in a browser |

## The Aha: local get, live write, host stays up

Kiponos holds a nested tree. Java and Python SDKs keep the latest values **in memory**, patched over WebSocket deltas. The hot path inside a MCP host tool is a **local get** — no HTTP RTT per admin dashboard lookup.

Hub leaf for this essay:

```text
examples/agentic-dev-0924-pm-agent-folder/owner-agent = live
```

Runnable proof: [`examples/java/agentic-dev-0924-pm-agent-folder`](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/agentic-dev-0924-pm-agent-folder)

Public SDKs: **Java**, **Python**, plus React/Angular **server** peers (`createFromEnv`). Never put Connect tokens in the SPA.

## Config tree (admin dashboard + peers)

```yaml
examples/
  agentic-dev-0924-pm-agent-folder/
    owner-agent: live          # Agents choose a metadata folder
apps/
  admin-dashboard/
    live:
      owner-agent: live
```

## Integration — Java hot path

```java
Kiponos kip = Kiponos.createForCurrentTeam();
Folder gate = kip.getRootFolder()
        .folderOrCreate("examples")
        .folderOrCreate("agentic-dev-0924-pm-agent-folder");
if (!gate.hasKey("owner-agent")) {
    gate.set("owner-agent", "live");
}
String posture = gate.get("owner-agent");
// MCP host tool: refuse the dangerous call when posture moved
```

Same leaf from a Python tool (MCP host just *calls* it):

```python
from kiponos import Kiponos

k = Kiponos.connect(quiet=True)  # env: KIPONOS_ID, KIPONOS_ACCESS, KIPONOS
try:
    posture = k.get("examples/agentic-dev-0924-pm-agent-folder/owner-agent", "live")
    if str(posture) == "live":
        raise PermissionError("Agents choose a metadata folder gated live — host not restarted")
finally:
    k.disconnect()
```

The MCP host **process** does not recycle. The **next** tool call already sees the dashboard edit.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| Status tile green while the agent host started earlier | Restart MCP host; lose the operator's last ten minutes of diagnosis | Set `owner-agent` live; next MCP host tool call already obeys |
| Peer host still on old owner-agent | Paste the value into the other chat | One hub leaf; both processes `get()` locally |
| admin-dashboard wall shows the new posture | MCP host started earlier so it writes anyway | Dashboard and tool share the same memory tree |
| Incident over, resume | Another MCP host restart | Set `owner-agent` back; session continues |
| Senses firing while the turn finishes blind | Two ceremonies, two lost threads | Same tree, two products, no paste |

## Performance (this path, not a generic table)

- MCP host tool `get()` is an in-process map lookup after bootstrap.
- One WebSocket per process lifetime — not per admin dashboard line.
- A dashboard edit is a **delta** of `owner-agent`, not a config-file reload.
- You do not pay model tokens to “please restart MCP host.”
- A second host converges without a third paste onto senses firing while the turn finishes blind.

## Compare to alternatives

| Approach | Honest fit | Why it still restarts |
|----------|------------|------------------------|
| Env file + MCP host reboot | Simple at 09:00 | The freeze is at 21:03 |
| Skill markdown as policy | Good instructions | Not a live bus |
| Redis poll inside the tool | Shared, but RTT on the hot path | You invented a hub with worse UX |
| Feature-flag SaaS | Product experiments | Rarely session-safe for MCP host |
| `@RefreshScope` / actuator | JVM apps | Does not restart MCP host |

## When not to use Kiponos

| Situation | Why |
|-----------|-----|
| Tool schema itself changed (new argument) | That *is* a code/MCP host restart |
| Secret rotation of Connect tokens | Credentials are not live knobs |
| One-off local script, no peers | A hub is overkill |
| Browser-only “SDK in the SPA” | Forbidden — tokens leak or defaults lie |

## Rehearsal beats slides

In staging: set a painful `owner-agent`, prove MCP host recovers without a host kill, prove clamps reject nonsense, prove last-known-good when the hub is firewalled. That drill ends half the architecture arguments about Agents choose a metadata folder.

## Why MCP host is the wrong restart target

MCP host is good at calling tools. It is not a control plane. Killing it to flip `owner-agent` teaches the on-call that judgment requires a process ID. The admin-dashboard wall already disagrees.

## What the admin dashboard operator actually said

At 21:03 someone said, out loud: **the tile is not a rumor. it is the same tree**. That sentence is the whole product. If it cannot land in the running MCP host process in seconds, you do not have posture. You have a wiki.


## Getting started (15 minutes)

1. TeamPro on [kiponos.io](https://kiponos.io) → Connect → `KIPONOS_ID` / `KIPONOS_ACCESS` / profile `['my-app']['v1.0.0']['dev']['base']`.
2. Clone [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io).
3. `cd examples/java/agentic-dev-0924-pm-agent-folder && cp kiponos.local.env.example kiponos.local.env`
4. `./gradlew test run` — prints `examples/agentic-dev-0924-pm-agent-folder/owner-agent=...`
5. In the dashboard, change `owner-agent`. Keep the process up. No rebuild.
6. Point your MCP host tool at the same leaf. Do not ship a new server binary to flip Agents choose a metadata folder.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

## The moral

If flipping **Agents choose a metadata folder** requires restarting MCP host, you do not have a gate. You have a hope with a process ID.

Agent frameworks already know how to call tools. **Kiponos is the live hub they do not ship** — so the admin-dashboard wall can change its mind without killing the session.

How to try: `examples/java/agentic-dev-0924-pm-agent-folder` and `./gradlew test`.
