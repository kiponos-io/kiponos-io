---
main_image: https://iili.io/Cb530oG.jpg
title: "The Agents Invented Their Own Folder. Then They Stopped Pasting State Into Chat."
published: false
tags: python, java, ai, architecture, devops
description: Agentic frameworks do not ship a place for agents to own shared metadata. With Kiponos SDK, Grok Build, Cursor, and Claude Code choose a live tree — Mirror Phone session and travel group-chat occupancy stay in hub memory, not in paste.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-agentic-metadata-chosen-tree.md
---

I have watched two coding agents argue about a travel-coordinator group chat like it was a Git conflict.

Grok Build had muted channel `ops-late-bags`. Claude Code, in another window, was still paging that channel because its “source of truth” was a bullet list someone pasted sixteen minutes ago. Cursor had a third copy in a scratch file named `state-final-v2.md`.

The Mirror Phone session that should have shown **which device was live** was the same mess: a string in a chat, a guess in a README, a dashboard that had been refreshed “recently.”

Nobody had given the agents a **folder they were allowed to own**.

So they did what frameworks teach them: they wrote prose. Prose is a terrible bus.

**The Aha:** let agents **choose their own live metadata tree** on [Kiponos.io](https://kiponos.io). Nested folders, typed keys, WebSocket deltas, local `get()` / `set()`. Grok Build, Cursor, and Claude Code become peers — not three diaries.

## The problem: shared state was a paste ritual

Agentic hosts are excellent at tools and terrible at **durable, instant, multi-peer memory**.

| Ritual | What it pretends to be | What it actually is |
|--------|------------------------|---------------------|
| Paste occupancy into the prompt | Handoff | Stale the moment the next message lands |
| Scratch markdown in the repo | Source of truth | A merge conflict with extra steps |
| Restart MCP with new env | Sync | Amnesia with a bash script |
| “The dashboard is the truth” | Ops | Only if the agent can `get()` it locally |

Travel coordinator group chats need **live** mute, occupancy, and routing. Mirror Phone needs **live** session owner and last-seen. Admin dashboards need the same leaves the agents write — without a human copying them.

## What teams believe

| Belief | Production |
|--------|------------|
| “The transcript is the state” | Transcripts are literature |
| “We’ll add a database later” | Later is this incident |
| “One agent owns disk, others read Git” | Git is not a pager |
| “Put tokens in the phone WebView” | Never — Mirror Phone talks to a **server** peer |

## The Aha: agents pick the folder, the hub fans it out

This is not “the platform assigned you `agent/global`.” This is **the agents naming a tree** that matches the product:

```text
examples/agentic-metadata-chosen-tree/owner-agent = travel-coordinator
```

They can also mkdir product folders — `mirror/session/*`, `travel/chats/<id>/*` — because the SDK creates parents. The dashboard shows it the same second. Another agent’s next `get()` is local.

Proof: [`examples/java/agentic-metadata-chosen-tree`](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/agentic-metadata-chosen-tree)

![Architecture diagram](https://iili.io/Cb53DfR.png)

## Config tree the agents chose

```yaml
examples/
  agentic-metadata-chosen-tree/
    owner-agent: travel-coordinator
    schema: v1
mirror/
  session/
    device-id: mirror-1
    live: yes
    last-seen-ms: "0"
travel/
  chats:
    ops-late-bags:
      occupancy: 14
      writes: muted
```

Keys stay small. No blobs. No secrets. Occupancy is a number; mute is a word; owner is a name the next agent can read without parsing a novel.

## Integration

Java (travel coordinator service + example):

```java
Kiponos kip = Kiponos.createForCurrentTeam();
Folder meta = kip.getRootFolder()
        .folderOrCreate("examples")
        .folderOrCreate("agentic-metadata-chosen-tree");
if (!meta.hasKey("owner-agent")) {
    meta.set("owner-agent", "travel-coordinator");
}
String owner = meta.get("owner-agent"); // local
```

Python — the agent *decides* the folder, then lives there:

```python
from kiponos import Kiponos

k = Kiponos.connect(quiet=True)
try:
    k.ensure_path("examples/agentic-metadata-chosen-tree")
    k.set("examples/agentic-metadata-chosen-tree/owner-agent", "travel-coordinator")
    k.ensure_path("travel/chats/ops-late-bags")
    k.set("travel/chats/ops-late-bags/writes", "muted")
    k.set("travel/chats/ops-late-bags/occupancy", "14")
finally:
    k.disconnect()
```

Cursor can `get("writes")` in an MCP tool without restarting Claude Code. Mirror Phone’s **server** SDK (`@kiponos/react` / Java BFF) reads `mirror/session/live` the same way. The phone never holds Connect tokens.

## Real scenarios

| Event | Paste world | Hub world |
|-------|-------------|-----------|
| Mute a travel group chat | Three agents, three copies | One `writes=muted` leaf |
| Mirror Phone switches device | Chat: “use the other phone” | `device-id` + `live=yes` |
| New agent joins the war room | Scroll the transcript | `get()` the folder the others chose |
| Occupancy hits 40 | Someone remembers to say it | Dashboard and MCP already show 40 |
| Owner handoff | @-mention archaeology | `owner-agent` changes; sessions stay up |

## Performance

- `set` is a delta; peers do not reload a document.
- `get` on the chat mute path is in-memory — safe inside a fan-out loop.
- Agents do not re-prompt a 20-message paste to recover occupancy.
- Admin dashboard is not a poller; it is the same tree.
- Mirror Phone BFF can render live without an extra “sync service.”

## Compare to alternatives

| Approach | Honest fit | Failure |
|----------|------------|---------|
| Chat as database | Tiny teams | Lies at minute sixteen |
| SQLite in one workspace | Single host | Cursor on another machine never sees it |
| Redis | Shared memory | You still build the product UX |
| Git notes | Audit | Not instant, not mid-turn |

## When not to use Kiponos

| Situation | Why |
|-----------|-----|
| Multi-megabyte transcripts | Hub leaves are knobs, not object storage |
| End-user PII in the clear | Do not invent a dossier in the tree |
| Need linear history of every paste | Use a log; the hub is latest truth |
| SPA talking straight to Connect | Forbidden — server peer only |

## Getting started (15 minutes)

1. Connect from [kiponos.io](https://kiponos.io).
2. Clone [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io).
3. `cd examples/java/agentic-metadata-chosen-tree && cp kiponos.local.env.example kiponos.local.env`
4. `./gradlew test run`
5. From any agent session, `set` `owner-agent` and a `travel/chats/...` mute key. Open the dashboard. Open a second agent. No restart. No paste.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

## The moral

If your agents are pasting state, they do not have state. They have folklore.

Give Grok Build, Cursor, and Claude Code a **folder they chose**, on a hub that fans out while the process still runs. Mirror Phone and travel group chats stop being rumors.

How to try: `examples/java/agentic-metadata-chosen-tree` and `./gradlew test`.
