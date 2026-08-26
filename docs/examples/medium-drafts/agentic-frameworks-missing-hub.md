# Agent Frameworks Gave Us Tools. They Did Not Give Us a Live Hub.

*A traveler’s note from a Cursor tab, a Claude Code window, and a Grok Build run that all restarted to share one boolean.*

---

I have sat between three agent hosts and a travel-coordinator admin wall that already knew the truth.

The group chat `ops-late-bags` was muted on the dashboard. Shopping checkout retries were supposed to be frozen. Mirror Phone was supposed to show which device was live. Every human in the room could *see* it.

The agents could not.

Grok Build had started an hour earlier. Cursor’s MCP server still had `write` enabled because that was the argv from lunch. Claude Code offered to “restart the tools” the way a well-meaning intern offers to reboot a router.

I have heard that sentence in more than one war room:

**“Let’s just restart the MCP server.”**

That is not an architecture. That is a shrug with a process ID.

The frameworks already know how to call tools. What they do not ship is a **live, nested, multi-peer tree** the tool can `get()` without dying. That missing piece is [Kiponos.io](https://kiponos.io).

<!-- medium-img: diagram-agentic-restart-vs-live.png -->

---

## What went wrong (the human version)

We treated agent hosts like twelve-factor apps from 2013: config is env, env is process-birth, birth is a restart.

That model is fine for a batch job. It is hostile to:

- a **Cursor** session that already spent the context window on a shopping incident
- a **Claude Code** tab that is mid-edit on the travel coordinator worker
- a **Grok Build** run that is three tools into “why is checkout retrying”

Restarting MCP to flip `tools-allow` or a sense priority throws away the only thing the model had going for it: **the turn**.

The dashboard was not the problem. The Java service was not the problem. The gap was between **mouth** and **every running peer**.

---

## The Super Pattern (live hub, not a new framework)

You do not replace Grok Build. You do not fork Cursor. You put a hub under the tools they already call.

Hub path:

```text
examples/agentic-frameworks-missing-hub/shared-truth = live
```

Local `get()` on the MCP hot path. Dashboard or another agent `set()` when the floor changes its mind. Java, Python, and server-side React/Angular peers share the same leaf. No “which host still has lunchtime argv?”

<!-- medium-img: diagram-agentic-hub-peers.png -->

---

## The example (standalone Java)

Published under:

**`examples/java/agentic-frameworks-missing-hub`** on [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/agentic-frameworks-missing-hub)

It connects with `Kiponos.createForCurrentTeam()`, ensures the folder, seeds a safe default, and prints the live leaf:

```java
public static void main(String[] args) throws Exception {
    Kiponos k = Kiponos.createForCurrentTeam();
    try {
        Folder p = k.getRootFolder()
                .folderOrCreate("examples")
                .folderOrCreate("agentic-frameworks-missing-hub");
        if (!p.hasKey("shared-truth")) {
            p.set("shared-truth", "live");
        }
        System.out.println("shared-truth=" + p.get("shared-truth"));
        Thread.sleep(1200L);
    } finally {
        k.disconnect();
    }
}
```

A Python MCP tool — the thing Grok Build / Cursor / Claude Code actually invoke — is the same contract:

```python
from kiponos import Kiponos

k = Kiponos.connect(quiet=True)
try:
    truth = k.get("examples/agentic-frameworks-missing-hub/shared-truth", "live")
    # travel coordinator / shopping admin: refuse writes when truth != live
finally:
    k.disconnect()
```

How to try:

```bash
cd examples/java/agentic-frameworks-missing-hub
cp kiponos.local.env.example kiponos.local.env   # kiponos.io → Connect
./gradlew test run
```

Then change `shared-truth` on the dashboard. Do **not** restart the agent host. The next tool call is enough.

---

## What this is for (and what it is not)

| Use it when | Do not use it when |
|-------------|-------------------|
| Tool allow-lists, sense priorities, chat mute, session owner | Tool *schema* changed (new args — that is a rebuild) |
| Shopping freeze, travel group-chat mute, Mirror Phone live device | Secrets / Connect tokens (never in a SPA, never as a “live knob”) |
| Two agent hosts must agree *now* | You wanted object storage for transcripts |

Public SDKs: Java, Python, `@kiponos/react` and `@kiponos/angular` as **server** peers. The admin dashboard and Mirror Phone BFF read memory. The browser does not hold the handshake.

---

## The war-room loop I wanted instead of a reboot

1. Floor says mute `ops-late-bags`.  
2. Someone sets the leaf (dashboard or an agent that owns metadata).  
3. Every MCP tool already running does a local `get()` on the next call.  
4. Shopping retries stop. Travel sends stop. Mirror Phone still shows the live device.  
5. Nobody types `/restart`.

That loop is boring. Boring is the point.

I have always preferred a boring hub to a clever restart. Agent frameworks got us the clever part — tools, skills, MCP. They left the boring part on the table. Pick it up.

---

## The moral

People should not have to kill a Cursor session to make a decision the dashboard already displayed.

If Grok Build, Claude Code, and the shopping admin wall disagree, you do not need a fourth framework. You need a **live hub** under the tools you already have.

Kiponos is that hub. The example is small on purpose. Run it. Then stop restarting MCP for a boolean.
