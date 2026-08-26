---
main_image: https://iili.io/Cb53IPR.jpg
title: "The Sense Fired Mid-Turn. The Agent Finished the Turn Anyway — Until the Hub Was Local."
published: false
tags: python, java, devops, ai, observability
description: Senses (network and ops probes) that live in files force the next agent turn — or a restart — to see reality. Kiponos SDK makes sense priority a live leaf, so Grok Build, Cursor, and Claude Code can decide mid-turn.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-agentic-senses-mid-turn.md
---

I have watched an agent keep calling a shopping checkout API while the sense on the desk already said **P1: upstream stall**.

The probe was not wrong. The **agent could not see it yet**.

The sense had written a JSON file. The MCP tool had read that file at process start. Grok Build was already in the middle of a turn — three tool calls deep into “retry the cart.” Cursor had a second session on the admin dashboard. Nobody wanted to restart the host *during* the turn. So the model finished a plan that the floor had already abandoned.

Someone said the quiet part:

> If a sense cannot interrupt the current turn, it is a log line. It is not a sense.

**The Aha:** a sense is an **operational leaf**. [Kiponos.io](https://kiponos.io) patches it into every SDK cache while the agent is still thinking. The next `get()` inside the same turn is enough. No MCP restart. No “start a new chat.”

## The problem: probes were faster than the agent’s world

Senses are simple when you say them in a war room:

- Shopping checkout p95 just went vertical  
- Admin dashboard WebSocket to the storefront is wedged  
- Travel coordinator’s group-chat fan-out is drowning the worker  

The probe can know in milliseconds. Agent frameworks, left alone, know when:

| Mechanism | When the agent finds out |
|-----------|--------------------------|
| File the MCP read at boot | Next process start |
| Prompt the human pastes | Next message — if anyone pastes |
| HTTP poll inside the tool | Every call pays RTT; still easy to skip mid-loop |
| Restart Cursor / Claude Code | You threw away the turn to import a boolean |

That is why senses *feel* late even when the probe was on time. The missing piece is not a smarter classifier. It is a **live shared tree** the turn can read without leaving the process.

## What teams believe

| Belief | Production |
|--------|------------|
| “The next turn will pick it up” | The damaging tool call is *this* turn |
| “Hooks will stop the agent” | Hooks help — if they read something live |
| “We’ll poll Redis” | You rebuilt a hub and put RTT on the hot path |
| “Restart is the only hard interrupt” | Restart is how you lose the diagnosis |

## The Aha: mid-turn local reads

Kiponos SDKs (Java, Python) keep the tree in memory. A sense publisher **sets** `priority`. WebSocket deltas merge. The agent’s MCP tool does a **local get** before the next checkout retry.

Hub leaf:

```text
examples/agentic-senses-mid-turn/priority = P3
```

Proof: [`examples/java/agentic-senses-mid-turn`](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/agentic-senses-mid-turn)

![Architecture diagram](https://iili.io/Cb53Lxa.png)

## Config tree

```yaml
examples/
  agentic-senses-mid-turn/
    priority: P1
    pattern: shopping-checkout-stall
    title: Checkout p95 vertical
senses/
  shopping:
    checkout-p95-ms: 4200
  admin:
    dashboard-ws: stalled
  travel:
    group-chat-backlog: high
```

The probe writes **leaves**. The agent does not parse a new config format mid-panic.

## Integration

Java service on the shopping side (and the same leaf the Python tool reads):

```java
Kiponos kip = Kiponos.createForCurrentTeam();
Folder sense = kip.getRootFolder()
        .folderOrCreate("examples")
        .folderOrCreate("agentic-senses-mid-turn");
if (!sense.hasKey("priority")) {
    sense.set("priority", "P3");
}

String p = sense.get("priority"); // local
if (p.startsWith("P0") || p.startsWith("P1")) {
    // do not fire the next checkout retry
}
```

Python MCP tool — called *inside* a Grok Build / Claude Code turn:

```python
from kiponos import Kiponos

def before_retry_checkout():
    k = Kiponos.connect(quiet=True)
    try:
        pr = str(k.get("examples/agentic-senses-mid-turn/priority", "P3"))
        if pr.startswith("P0") or pr.startswith("P1"):
            return {"abort": True, "reason": f"sense {pr} live — no restart"}
        return {"abort": False}
    finally:
        k.disconnect()
```

Optional: `on_change` so a long-running host can set a process-local flag the moment the delta arrives — still no MCP reboot.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| Shopping checkout stall | Agent retries 8 more times this turn | Mid-turn `get()` aborts retries |
| Admin dashboard WS stall | New Cursor chat to “please stop” | Same leaf; both hosts see P1 |
| Travel group-chat flood | Restart the worker to change a threshold | Threshold is a leaf; worker keeps the session |
| Sense was a false positive | Another restart to lower priority | Set `P3`; next get continues work |
| Two probes, one agent | Whose file wins? | One folder, last delta is truth |

## Performance

- Sense check on the hot path is a map lookup, not a probe round-trip.
- Probes can stay dumb: they **set** keys; they do not wait for agents.
- Mid-turn means **before the next tool call**, not after the model essay.
- You do not spend a restart — or a new 40k-token chat — to import a priority.
- Admin dashboard and agent MCP share one tree; no dual-write.

## Compare to alternatives

| Approach | Honest fit | Gap |
|----------|------------|-----|
| JSON file + inotify | Laptop demos | MCP sandboxes and remote hosts miss it |
| Stdout logs the human reads | Forensics | Not a mid-turn gate |
| HTTP poll per tool call | Shared | RTT in the hottest loop |
| Restart the agent host | Nuclear interrupt | Throws away the only context you had |

## When not to use Kiponos

| Situation | Why |
|-----------|-----|
| Sense requires killing the **kernel** path | That is not config |
| You need a guaranteed POSIX signal in 1ms | Use a real interrupt; hub is “next get” |
| Classifying malware in the probe payload | Do not put blobs in the live tree |
| Browser-held Connect tokens | Never — server SDK only |

## Getting started (15 minutes)

1. Connect tokens + profile from [kiponos.io](https://kiponos.io).
2. Clone [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io).
3. `cd examples/java/agentic-senses-mid-turn && cp kiponos.local.env.example kiponos.local.env`
4. `./gradlew test run`
5. Set `priority=P1` on the dashboard. The next local `get()` in your MCP tool should abort the shopping retry — **same turn**, same process.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

## The moral

A sense that cannot change the **current** turn is a diary.

Agent frameworks will happily finish a bad plan unless something **already in process memory** says stop. Kiponos is that memory — for shopping checkout, admin dashboards, and travel group-chat floods — without restarting Grok Build, Cursor, or Claude Code.

How to try: `examples/java/agentic-senses-mid-turn` and `./gradlew test`.
