# Agent Token Budgets You Can Tighten Mid-Run

*A traveler’s note from the multi-SDK mesh: Java, Python, React, Angular — and agents that never reboot for a leaf.*

---

There is a class of production decisions that are **too small for a release** and **too important for a chat thread**.

A research agent burned the monthly LLM budget in one night because the cap lived in a restarted config file nobody wanted to bounce.

Someone said the sentence that always costs a night:

**“Budgets that need a restart are not budgets. They are post-mortems.”**

That sentence is the product brief for **live max tokens per turn**.

I have always believed people should not have to ship a release to make a decision. With **Java**, **Python**, **@kiponos/react** (Node server peer), and **@kiponos/angular** (Node server peer) on one living tree, the brief finally has four honest voices — plus agents that read the same leaves without killing their sessions.

Agents are not a side channel. When skills, MCP tool gates, token budgets, or handoff tickets live
on the same hub tree as services and BFFs, **nobody needs a restart to collaborate**. The session stays.
The leaf moves. That is the agentic half of this revolution — not chatbots bolted on, but real-time
control for long-running workers.

---

## What went wrong (the human version)

Distributed systems did not lack languages. They lacked a **shared operational plane** that all peers could honor while running.

| Old habit | What it costs |
|-----------|----------------|
| YAML per service | Four PRs to flip one truth |
| Restart the agent to enable a skill | Lost context, lost time |
| Put the SDK in the browser | Leaked tokens or stale defaults |
| MCP tools always on | Incidents with no real gate |
| Separate ops wall from agents | Three “truths” in one war room |

The Super Pattern is simpler: put **live max tokens per turn** (`max-tokens-per-turn`) on [Kiponos.io](https://kiponos.io). Every peer uses **local `get()`** after bootstrap. Dashboard, agent, or any SDK `set()` when judgment arrives.

<!-- medium-img: diagram-agent-budget-token-live-gof-vs-live.png -->

---

## The Super Pattern (process identity, multi-peer)

Hub leaf (this example):

```text
examples/agent-budget-token-live/max-tokens-per-turn = 4000
```

Java peer:

```java
Kiponos k = Kiponos.createForCurrentTeam();
try {
    Folder f = k.getRootFolder()
        .folderOrCreate("examples")
        .folderOrCreate("agent-budget-token-live");
    String v = f.hasKey("max-tokens-per-turn") ? f.get("max-tokens-per-turn") : "4000";
    System.out.println("max-tokens-per-turn=" + v);
} finally {
    k.disconnect();
}
```

Python agent peer (session stays up):

```python
# moral: connect once; read leaf live; do not restart to flip posture
from kiponos import Kiponos
k = Kiponos.connect(quiet=True)
# path/get style depends on kit — leaf is examples/agent-budget-token-live/max-tokens-per-turn
print("agent online; flip the leaf on the hub without killing this process")
```

React **server** peer (never browser Connect tokens):

```ts
import { Kiponos } from '@kiponos/react/server';

const kip = Kiponos.createFromEnv();
await kip.connect();
await kip.ensurePath('examples/agent-budget-token-live');
// mirror to SPA via your API/SSE — SPA is a client of you, not of Connect
const v = /* local get after bootstrap */ '4000';
```

Angular **server** peer — same moral as React: process identity, thin UI mirror.

<!-- medium-img: diagram-agent-budget-token-live-hub-flow.png -->

---

## The example (runnable)

Published under **`examples/java/agent-budget-token-live`** on [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/agent-budget-token-live)

Python companion: `examples/python/agent-budget-token-live`

Peers in this story: **python, java**

Public surface: **[kiponos.io](https://kiponos.io)** and the public GitHub tree.  
Do **not** publish private team path trees or private product URLs.

---

## Install & how to try

```bash
# Java
cd examples/java/agent-budget-token-live
cp kiponos.local.env.example kiponos.local.env   # Connect tokens from kiponos.io
./gradlew test run

# Python agent peer
cd examples/python/agent-budget-token-live
# export KIPONOS_ID KIPONOS_ACCESS KIPONOS=...
python3 agent_peer.py

# React / Angular server peers
npm install @kiponos/react    # or @kiponos/angular
# createFromEnv / createForCurrentTeam in Node — never in the SPA bundle
```

## Old world vs live multi-SDK mesh

| Move | Old world | Live mesh |
|------|-----------|-----------|
| Flip live max tokens per turn | Redeploy N services | One hub `set` |
| Enable agent skill / MCP tool | Restart agent host | Leaf gate |
| Align UI with server | Hope the SPA build matches | BFF mirrors hub |
| Debug prod | DEBUG=1 + bounce | Live probe leaf |
| War room truth | Slack + three dashboards | One hub headline |

---

## Guardrails (traveler’s checklist)

1. Prefer `createForCurrentTeam` / `createFromEnv` over token constructors.  
2. **Never** put Connect tokens in SPA bundles — React/Angular SDKs are **server** peers.  
3. Agents and MCP hosts are peers too: skills, tools, budgets, handoffs are leaves.  
4. Fail closed on money and kill paths when the leaf is missing.  
5. Measure success in **seconds from judgment to effect** across *all* peers.  
6. Never publish private family/agent path trees in public articles.  
7. Rehearse a multi-peer proof (Java set → Python agent read → BFF mirror) before the next “just restart it” meeting.

| Do | Don't |
|----|-------|
| One profile tree for Java/Python/React/Angular/agents | Four config systems |
| Local get on hot path | Poll hub per request |
| Live skill/MCP/budget gates | Restart to flip a flag |
| UI as mirror via your API | Browser as hub participant |

---

## The moral

**People should not have to ship a release to make a decision** — and agents should not die to learn a new skill, gate, or budget.

Identity is **where the process runs**. The mesh is **one living tree**. Java, Python, React, Angular, and agents are peers. Change `max-tokens-per-turn`. Watch every honest peer obey. Keep the release train for real code.

---

## The lie we stop telling

“We’ll align the stack in the next multi-service deploy.”

No. We’ll put **live max tokens per turn** on the hub, put **identity** on each process, let agents stay warm, and let every SDK speak the same leaf while the world is still on fire.

---

*Runnable: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/agent-budget-token-live](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/agent-budget-token-live)*
