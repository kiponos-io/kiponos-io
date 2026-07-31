# MCP Tool Gates Without Restarting the Agent Host

*A traveler’s note from the multi-SDK mesh: Java, Python, React, Angular — and agents that never reboot for a leaf.*

---

There is a class of production decisions that are **too small for a release** and **too important for a chat thread**.

A write tool stayed enabled during a customer bridge call because restarting the host would drop the session.

Someone said the sentence that always costs a night:

**“If the tool gate needs a restart, it is not a gate. It is a hope.”**

That sentence is the product brief for **live MCP tool allow-list**.

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

The Super Pattern is simpler: put **live MCP tool allow-list** (`tools-allow`) on [Kiponos.io](https://kiponos.io). Every peer uses **local `get()`** after bootstrap. Dashboard, agent, or any SDK `set()` when judgment arrives.

<!-- medium-img: diagram-mcp-tool-gate-live-gof-vs-live.png -->

---

## The Super Pattern (process identity, multi-peer)

Hub leaf (this example):

```text
examples/mcp-tool-gate-live/tools-allow = read,search
```

Java peer:

```java
Kiponos k = Kiponos.createForCurrentTeam();
try {
    Folder f = k.getRootFolder()
        .folderOrCreate("examples")
        .folderOrCreate("mcp-tool-gate-live");
    String v = f.hasKey("tools-allow") ? f.get("tools-allow") : "read,search";
    System.out.println("tools-allow=" + v);
} finally {
    k.disconnect();
}
```

Python agent peer (session stays up):

```python
# moral: connect once; read leaf live; do not restart to flip posture
from kiponos import Kiponos
k = Kiponos.connect(quiet=True)
# path/get style depends on kit — leaf is examples/mcp-tool-gate-live/tools-allow
print("agent online; flip the leaf on the hub without killing this process")
```

React **server** peer (never browser Connect tokens):

```ts
import { Kiponos } from '@kiponos/react/server';

const kip = Kiponos.createFromEnv();
await kip.connect();
await kip.ensurePath('examples/mcp-tool-gate-live');
// mirror to SPA via your API/SSE — SPA is a client of you, not of Connect
const v = /* local get after bootstrap */ 'read,search';
```

Angular **server** peer — same moral as React: process identity, thin UI mirror.

<!-- medium-img: diagram-mcp-tool-gate-live-hub-flow.png -->

---

## The example (runnable)

Published under **`examples/java/mcp-tool-gate-live`** on [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/mcp-tool-gate-live)

Python companion: `examples/python/mcp-tool-gate-live`

Peers in this story: **python, java**

Public surface: **[kiponos.io](https://kiponos.io)** and the public GitHub tree.  
Do **not** publish private team path trees or private product URLs.

---

## Install & how to try

```bash
# Java
cd examples/java/mcp-tool-gate-live
cp kiponos.local.env.example kiponos.local.env   # Connect tokens from kiponos.io
./gradlew test run

# Python agent peer
cd examples/python/mcp-tool-gate-live
# export KIPONOS_ID KIPONOS_ACCESS KIPONOS=...
python3 agent_peer.py

# React / Angular server peers
npm install @kiponos/react    # or @kiponos/angular
# createFromEnv / createForCurrentTeam in Node — never in the SPA bundle
```

## Old world vs live multi-SDK mesh

| Move | Old world | Live mesh |
|------|-----------|-----------|
| Flip live MCP tool allow-list | Redeploy N services | One hub `set` |
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

Identity is **where the process runs**. The mesh is **one living tree**. Java, Python, React, Angular, and agents are peers. Change `tools-allow`. Watch every honest peer obey. Keep the release train for real code.

---

## The lie we stop telling

“We’ll align the stack in the next multi-service deploy.”

No. We’ll put **live MCP tool allow-list** on the hub, put **identity** on each process, let agents stay warm, and let every SDK speak the same leaf while the world is still on fire.

---

*Runnable: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/mcp-tool-gate-live](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/mcp-tool-gate-live)*
