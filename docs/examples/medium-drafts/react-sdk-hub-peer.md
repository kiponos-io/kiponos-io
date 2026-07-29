# The Fourth Voice on the Hub Was Not Another JVM

*A traveler’s note from a living config tree: the Node peer that finally spoke the same language as Java.*

---

There is a class of production decisions that are **too small for a release** and **too important for a chat thread**.

I have sat in rooms where the backend already had three kinds of participants on the Kiponos hub:

1. Humans on the **dashboard**  
2. **Java** services with `createForCurrentTeam()`  
3. **Python** agents writing ops state  

What we did **not** have was a first-class **Node** peer with the same contract: env identity, in-memory tree, live deltas — while a React UI stayed thin and honest.

Redeploying a frontend bundle to change `demo/status-wall/status-alpha` is how teams invent folklore. Someone ships a SPA build to flip a shared string. Tokens leak into public JS because the file was named `.env`. The browser becomes a participant it was never meant to be.

That is not real-time config. That is a ceremony with a webpack graph attached.

---

## What went wrong (the human version)

A SaaS “React app” is usually two things people mash into one word:

| Piece | Runs where | Holds Connect tokens? |
|-------|------------|------------------------|
| SPA bundle | Visitor’s browser | **Never** |
| Node (or any) API | Your machine / cluster | **Yes — like Java** |

The engineers were not stupid. The UI needed live status. The mistake was treating the **browser** as the hub participant.

I once heard a lead say, half joking, half broken:

**“If the Node process could write the wall the way the JVM reads it, I’d stop shipping SPA builds for ops strings.”**

That sentence is the whole product brief.

<!-- medium-img: diagram-spa-vs-node.png -->

---

## The Super Pattern (process identity, not a SPA secret)

The React SDK is honest about where identity lives:

```js
import { Kiponos } from '@kiponos/react/server';

const kip = Kiponos.createFromEnv(); // or createForCurrentTeam()
await kip.connect();
await kip.path("demo", "status-wall").set("status-alpha", "focus");
```

No constructor tokens in the browser. Process env only — the same idea as Java’s singleton.

Hub tree:

```text
demo / status-wall / status-alpha = online
demo / status-wall / status-beta  = home
demo / status-wall / note         = live
demo / status-wall / last-ping    = <iso>
```

Local `get()` on the hot path. Dashboard, Java, Python, or Node `set()` when the world changes. The SPA talks **SSE to your Node process**, not Connect tokens to the hub.

That is the Super Pattern for frontends:

> Live hub + process identity + thin UI mirror = decisions that move without a release **or** a leaked token.

<!-- medium-img: diagram-hub-peers.png -->

---

## The example (Java still peers with Node)

Published under:

**`examples/java/react-sdk-hub-peer`** on [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/react-sdk-hub-peer)

The Java peer reads the same leaves the Node process wrote:

```java
Kiponos k = Kiponos.createForCurrentTeam();
try {
    Folder wall = k.getRootFolder()
        .folderOrCreate("demo")
        .folderOrCreate("status-wall");
    String alpha = wall.hasKey("status-alpha")
        ? wall.get("status-alpha")
        : "—";
    System.out.println("status-alpha=" + alpha);
    // Node createFromEnv wrote this seconds ago — no redeploy
} finally {
    k.disconnect();
}
```

Node side (the fourth voice):

```js
const kip = Kiponos.createForCurrentTeam();
await kip.connect();
await kip.path("demo", "status-wall").set("status-alpha", "focus");
await kip.path("demo", "status-wall").set("last-ping", new Date().toISOString());
```

Two tabs. One status flip. Everyone sees it — including a JVM that never restarted.

Public surface: **[kiponos.io](https://kiponos.io)** (hub + docs) and the example tree on GitHub.  
Do **not** treat private team homes or private ops walls as public demo URLs.

---

## Install (npm)

```bash
npm install @kiponos/react
```

Public package: https://www.npmjs.com/package/@kiponos/react  
Runnable Node example: `examples/node/react-status-wall`  
Source: https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-react-sdk

## How to try

```bash
# 1) Install the SDK from npm
npm install @kiponos/react

# 2) Connect credentials (same as Java)
export KIPONOS_ID=… KIPONOS_ACCESS=…
export KIPONOS="['MyApp']['1.0']['Dev']['base']"

# 3) Runnable Node peer
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/node/react-status-wall
npm install
npm start
# → writes demo/status-wall/* on the hub

# 4) Optional Java peer (same tree)
cd ../../java/react-sdk-hub-peer
# export same KIPONOS_* then run the Java example / tests
```

Package: [@kiponos/react on npm](https://www.npmjs.com/package/@kiponos/react)

## Old world vs live hub

| Move | Old world (SPA as participant) | Live hub (Node peer) |
|------|--------------------------------|----------------------|
| Flip a status string | Ship SPA build | Node / dashboard `set()` |
| Where tokens live | Often public JS | Process env only |
| Java sees the change | After redeploy folklore | Same tree, live deltas |
| Browser role | Fake hub client | Client of **your** API / SSE |

---

## A traveler’s checklist

1. Prefer `createForCurrentTeam` / `createFromEnv` over token constructors.  
2. Keep Connect tokens next to other production secrets — never in a bundler input that ships to visitors.  
3. Bridge browsers with SSE/API you control.  
4. Let the dashboard remain the human surface; let SDKs remain service surfaces.  
5. Measure success by **seconds from judgment to effect**, not by how many languages have a client library.  
6. Never publish private team path trees or private product URLs in public articles.  
7. Rehearse a two-tab proof (Node write → Java read) before the next “just put the SDK in the frontend” meeting.

---

## The moral

**People should not have to ship a release to make a decision** — and they should not paste service tokens into a SPA to share state.

Identity is **where the process runs**. The fourth voice is Node. The UI can stay thin, live, and honest.

Ship the peer once. Leave the bundle alone when the only thing that changed is a shared operational string.

---

## The lie we stop telling

“We’ll just put the SDK in the frontend.”

No. We’ll put **live state** in the hub, put **identity** on the process, and let the frontend stay a fast mirror.

---

*Example + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/react-sdk-hub-peer](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/react-sdk-hub-peer)*
