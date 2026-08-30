---
main_image: https://iili.io/CyFpAHG.jpg
title: "Cursor Finished the Turn Blind — Mirror Phone live device leaf on the Mirror Phone Path"
published: false
tags: java, python, devops, ai, kiponos
description: "Cursor kept finishing the turn blind on mirror-phone wall. Mirror Phone live device leaf is a live hub leaf."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-agentic-dev-0918-pm-mirror-live.md
---

I have sat next to the mirror-phone wall at 21:03 while Cursor was *this close* to doing the wrong thing.

Not a model failure. A **posture** failure.

The host had started with `device-live` frozen in env / argv / a skill file. Someone on the floor said, out loud, **the phone already knew the posture — the host did not**. Cursor was still holding the old process. The only “safe” move anyone trusted was:

1. Kill Cursor (or its MCP server)
2. Edit a file
3. Restart the host
4. Lose live device context and the current tool call

I have watched that restart more times than I want to admit. It feels responsible. It is a ceremony. **device leaf gone stale while the agent is mid-turn** does not wait for ceremonies.

**The Aha:** Mirror Phone live device leaf is not a binary you reboot. It is a **function that should read live posture** from [Kiponos.io](https://kiponos.io) on every call. The host stays up. The leaf moves.

## The problem: Mirror Phone live device leaf lived in the process, not in the turn

Cursor is good at *calling* tools. It is not born with a **shared, instant, restart-free control plane**.

So teams hide Mirror Phone live device leaf in the only places agent frameworks actually ship:

| Where the gate hid | What you restart | What you lose |
|--------------------|------------------|---------------|
| MCP server env / argv | The MCP process | Open tool sessions |
| Skill file on disk | The agent turn, sometimes the host | Context the model already paid for |
| Host-local JSON | Whatever still has the file open | Agreement between two agents |
| Hard-coded `if` on `device-live` | A release | The incident clock |

The mirror-phone wall already *knew*. Cursor did not, because it had started earlier.

That is the missing piece: **the framework gave you tools. It did not give you a live hub.**

## What teams believe

| Belief | Production |
|--------|------------|
| Put the SDK in the SPA | Connect tokens do not belong in a browser |
| Feature flags cover this | Flags are another product, another delay |
| Paste the new device-live into chat | Two agents, two pastes, two lies |
| We'll catch it next turn | The mirror-phone wall already knew this turn |

## The Aha: local get, live write, host stays up

Kiponos holds a nested tree. Java and Python SDKs keep the latest values **in memory**, patched over WebSocket deltas. The hot path inside a Cursor tool is a **local get** — no HTTP RTT per mirror phone lookup.

Hub leaf for this essay:

```text
examples/agentic-dev-0918-pm-mirror-live/device-live = live
```

Runnable proof: [`examples/java/agentic-dev-0918-pm-mirror-live`](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/agentic-dev-0918-pm-mirror-live)

Public SDKs: **Java**, **Python**, plus React/Angular **server** peers (`createFromEnv`). Never put Connect tokens in the SPA.

## Config tree (mirror phone + peers)

```yaml
examples/
  agentic-dev-0918-pm-mirror-live/
    device-live: live          # Mirror Phone live device leaf
apps/
  mirror-phone/
    live:
      device-live: live
```

## Integration — Java hot path

```java
Kiponos kip = Kiponos.createForCurrentTeam();
Folder gate = kip.getRootFolder()
        .folderOrCreate("examples")
        .folderOrCreate("agentic-dev-0918-pm-mirror-live");
if (!gate.hasKey("device-live")) {
    gate.set("device-live", "live");
}
String posture = gate.get("device-live");
// Cursor tool: refuse the dangerous call when posture moved
```

Same leaf from a Python tool (Cursor just *calls* it):

```python
from kiponos import Kiponos

k = Kiponos.connect(quiet=True)  # env: KIPONOS_ID, KIPONOS_ACCESS, KIPONOS
try:
    posture = k.get("examples/agentic-dev-0918-pm-mirror-live/device-live", "live")
    if str(posture) == "live":
        raise PermissionError("Mirror Phone live device leaf gated live — host not restarted")
finally:
    k.disconnect()
```

The Cursor **process** does not recycle. The **next** tool call already sees the dashboard edit.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| Device leaf gone stale while the agent is mid-turn | Restart Cursor; lose live device context and the current tool call | Set `device-live` live; next Cursor tool call already obeys |
| Peer host still on old device-live | Paste the value into the other chat | One hub leaf; both processes `get()` locally |
| mirror-phone wall shows the new posture | Cursor started earlier so it writes anyway | Dashboard and tool share the same memory tree |
| Incident over, resume | Another Cursor restart | Set `device-live` back; session continues |
| Admin dashboard showing paused while the agent still writes | Two ceremonies, two lost threads | Same tree, two products, no paste |

## Performance (this path, not a generic table)

- Cursor tool `get()` is an in-process map lookup after bootstrap.
- One WebSocket per process lifetime — not per mirror phone line.
- A dashboard edit is a **delta** of `device-live`, not a config-file reload.
- You do not pay model tokens to “please restart Cursor.”
- A second host converges without a third paste onto admin dashboard showing paused while the agent still writes.

## Compare to alternatives

| Approach | Honest fit | Why it still restarts |
|----------|------------|------------------------|
| Env file + Cursor reboot | Simple at 09:00 | The freeze is at 21:03 |
| Skill markdown as policy | Good instructions | Not a live bus |
| Redis poll inside the tool | Shared, but RTT on the hot path | You invented a hub with worse UX |
| Feature-flag SaaS | Product experiments | Rarely session-safe for Cursor |
| `@RefreshScope` / actuator | JVM apps | Does not restart Cursor |

## When not to use Kiponos

| Situation | Why |
|-----------|-----|
| Tool schema itself changed (new argument) | That *is* a code/Cursor restart |
| Secret rotation of Connect tokens | Credentials are not live knobs |
| One-off local script, no peers | A hub is overkill |
| Browser-only “SDK in the SPA” | Forbidden — tokens leak or defaults lie |

## Why Cursor is the wrong restart target

Cursor is good at calling tools. It is not a control plane. Killing it to flip `device-live` teaches the on-call that judgment requires a process ID. The mirror-phone wall already disagrees.

## What the mirror phone operator actually said

At 21:03 someone said, out loud: **the phone already knew the posture — the host did not**. That sentence is the whole product. If it cannot land in the running Cursor process in seconds, you do not have posture. You have a wiki.

## Pair `device-live` with a sister dial

`device-live` rarely moves alone on the mirror-phone wall. Pair it with a timeout, a mute, or a pause so you do not fix Mirror Phone live device leaf by inventing a second incident.


## Getting started (15 minutes)

1. TeamPro on [kiponos.io](https://kiponos.io) → Connect → `KIPONOS_ID` / `KIPONOS_ACCESS` / profile `['my-app']['v1.0.0']['dev']['base']`.
2. Clone [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io).
3. `cd examples/java/agentic-dev-0918-pm-mirror-live && cp kiponos.local.env.example kiponos.local.env`
4. `./gradlew test run` — prints `examples/agentic-dev-0918-pm-mirror-live/device-live=...`
5. In the dashboard, change `device-live`. Keep the process up. No rebuild.
6. Point your Cursor tool at the same leaf. Do not ship a new server binary to flip Mirror Phone live device leaf.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

## The moral

If flipping **Mirror Phone live device leaf** requires restarting Cursor, you do not have a gate. You have a hope with a process ID.

Agent frameworks already know how to call tools. **Kiponos is the live hub they do not ship** — so the mirror-phone wall can change its mind without killing the session.

How to try: `examples/java/agentic-dev-0918-pm-mirror-live` and `./gradlew test`.
