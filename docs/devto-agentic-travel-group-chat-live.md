---
main_image: https://iili.io/Cb5KdYB.jpg
title: "Mute the Flooded Travel Group Chat Without Killing the Agent"
published: false
tags: java, python, kiponos, ai
description: "Travel Coordinator App agents mute a noisy group-chat channel via Kiponos chat-mute — write-tools pause, MCP host stays up, delayed-flight chaos stays contained."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-agentic-travel-group-chat-live.md
---

**The Aha:** a Travel Coordinator App with real-time group chats does not need a heroic agent that “handles everything.” It needs a **mute leaf** — so write-tools on the flooding channel stop while occupancy, routing, and the rest of the agent session keep breathing.

I have heard the pager tone that means a delayed flight just became a chat storm. Forty travelers in one group thread. An agent (Grok Build or Claude Code) still had write-tools enabled. It tried to be helpful — three posts, then six, then the thread ate the night. Someone yelled “kill the agent.” That stopped the noise and also threw away the live occupancy map mid-rebuild.

> Mute the channel. Do not execute the agent.

Hub key: `chat-mute` under `examples/agentic-travel-group-chat-live/`.

## The problem: write-tools with no live brake

Agents wired into a Travel Coordinator App usually gate tools with a static allow-list:

```yaml
# application.yml — frozen until redeploy
travel:
  group-chat:
    write-tools: true
    mute-channels: []   # always empty in prod folklore
```

Or a Python constant the MCP host loads once at boot:

```python
CHAT_MUTE = "none"  # reboot MCP to change — travelers keep typing
```

When a delayed flight floods `#gate-b12`, you already know the answer: **mute write-tools for that channel**. What you lack is a path from judgment → every agent peer that does not require killing the MCP host, redeploying the Travel service, or pasting a new env into a SPA that must never hold Connect tokens.

## What teams believe vs production

| Belief | Production reality |
|--------|--------------------|
| Kill the agent to stop spam | Occupancy + routing context die with the process |
| Feature flag covers mute | Second system, second delay, second outage mode |
| Restart MCP to reload tools | Session memory gone; flood continues during reboot |
| Put mute in the traveler SPA | Tokens leak — or the UI shows a mute the agents ignore |

## The Aha: mute is operational posture on the hub

[Kiponos.io](https://kiponos.io) is a real-time config hub: one nested tree per profile, WebSocket deltas into each SDK’s **in-memory** cache. Hot-path reads (`get`) never pay hub RTT. Humans and agents are peers on the same tree.

For this story, on-call sets `chat-mute` to the flooding channel id (or a CSV). Every Grok Build / Claude Code tool loop **locally** sees the mute on the next call. Write-tools for that channel no-op. Read tools, occupancy leaves, and routing leaves keep working. The MCP host stays up.

| SDK | Role in the Travel Coordinator App |
|-----|------------------------------------|
| **Java** | Group-chat service + occupancy APIs |
| **Python** | Agent / MCP tool loop mid-turn |
| **React (`@kiponos/react`)** | **Node** BFF for coordinator console — never browser tokens |
| **Angular (`@kiponos/angular`)** | Same server-peer moral |

## Architecture

![Architecture diagram](https://iili.io/Cb5KKTF.png)

## Config tree (this story)

```yaml
examples:
  agentic-travel-group-chat-live:
    chat-mute: "none"              # channel id or CSV; none = unmuted
    occupancy-live: "true"         # keep reading seats / rooms
    routing-mode: "rebook-assist"  # agent playbook leaf
    write-tools-default: "on"      # clamped by chat-mute per channel
    mute-reason: ""
    set-by: oncall
    set-at: "<iso>"
    failClosed: true
```

Profile path shape: `['my-app']['v1.0.0']['dev']['base']` — from Connect on [kiponos.io](https://kiponos.io).

## Integration — Java hot path

```java
Kiponos kip = Kiponos.createForCurrentTeam();
Folder f = kip.getRootFolder()
    .folderOrCreate("examples")
    .folderOrCreate("agentic-travel-group-chat-live");
String mute = f.hasKey("chat-mute") ? f.get("chat-mute") : "none";
// group-chat write path
if (!"none".equalsIgnoreCase(mute) && muteContains(mute, channelId)) {
    return; // mute write — do not kill the agent session
}
// occupancy / routing reads continue via other leaves
```

Runnable: [examples/java/agentic-travel-group-chat-live](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/agentic-travel-group-chat-live)

## Integration — Python agent / MCP tool

```python
from kiponos import Kiponos

k = Kiponos.connect(quiet=True)
folder = (
    k.get_root_folder()
    .folder_or_create("examples")
    .folder_or_create("agentic-travel-group-chat-live")
)
mute = folder.get("chat-mute") if folder.has_key("chat-mute") else "none"

def may_write(channel_id: str) -> bool:
    if mute.strip().lower() in ("", "none"):
        return True
    return channel_id not in {c.strip() for c in mute.split(",")}
# MCP host stays up — only write-tools for the muted channel soft-fail
```

## Integration — React / Angular server peers

```ts
import { Kiponos } from '@kiponos/react/server'; // Angular: @kiponos/angular
const kip = Kiponos.createFromEnv();
await kip.connect(); // mirror chat-mute via your API/SSE — never SPA Connect tokens
```

## Real scenarios

| Event | Without live mute | With Kiponos `chat-mute` |
|-------|-------------------|--------------------------|
| Delayed flight floods `#gate-b12` | Agent keeps posting; humans rage-quit | Mute that channel; session continues |
| Need occupancy while muted | Kill agent → lose map | Reads stay live on other leaves |
| Second channel still calm | Global “disable writes” hammer | Per-channel mute CSV |
| On-call lifts mute after gate reopen | Redeploy / MCP reboot folklore | Flip leaf back to `none` |
| Handoff to another agent | Paste mute state | Same leaf — no ceremony |

## Performance notes (this use case)

- Every outbound chat write checks mute from **local memory** after bootstrap — not a hub poll per message.
- Deltas reach Java services and Python MCP tools together; no “which peer still unmuted?” lag hunt.
- Coordinator console browsers talk to **your** BFF/SSE, keeping Connect tokens off traveler devices.
- Muting one channel does not restart tool hosts — token/context budgets survive the incident.

## Compare to alternatives

| Approach | Honest fit | Gap |
|----------|------------|-----|
| Kill agent / MCP reboot | Stops spam once | Destroys session + occupancy work |
| Env YAML + redeploy | Versioned defaults | Too slow for airport time |
| Redis pub/sub DIY | Possible | You own the control plane forever |
| Product feature flags | Experiments | Often not mid-turn agent-safe |
| **Kiponos multi-SDK mesh** | Live mute + routing peers | Not a chat transcript store; not Vault |

## When not to use Kiponos

| Situation | Prefer |
|-----------|--------|
| Storing full group-chat message history | Your chat datastore / queue |
| Cryptographic secrets for Travel APIs | Vault / KMS |
| Changing DB schema for bookings | Versioned migration deploy |
| Anonymous marketing A/B on the booking site | Product experiment platform |

## Getting started (15 minutes)

1. Free hub profile at [kiponos.io](https://kiponos.io) → Connect → copy id, access, profile.  
2. Clone [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io).  
3. `cd examples/java/agentic-travel-group-chat-live && cp kiponos.local.env.example kiponos.local.env`.  
4. `./gradlew test run` — prints `chat-mute`.  
5. Simulate flood: set `examples/agentic-travel-group-chat-live/chat-mute` to `gate-b12` on the dashboard.  
6. Leave the MCP / agent process running — write-tools for `gate-b12` should soft-fail; occupancy reads continue.

## How to try

```bash
cd examples/java/agentic-travel-group-chat-live
cp kiponos.local.env.example kiponos.local.env
./gradlew test
./gradlew run
```

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

## The moral

**Flooded group chats are a mute problem, not an execution problem.** Put `chat-mute` on the hub. Keep the Travel Coordinator agent session and MCP host alive. Silence the noisy channel. Let occupancy and routing leaves keep working while the airport sorts the delay.

## Closing

Live chat mute belongs on [Kiponos.io](https://kiponos.io) — not in a killed agent, not in a redeploy window, not in a traveler-facing secret. Mute the channel. Keep the session.
