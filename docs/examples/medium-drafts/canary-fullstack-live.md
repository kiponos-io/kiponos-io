# Canary Percent That Moves BFF, Workers, and UI Mirrors Together

*A traveler’s note on the real product magic: **Web, Server, and Users on the same Team tree**, moving together in real time — Java, Python, React, and Angular as honest peers.*

---

There is a class of production decisions that are **too small for a release** and **too important for a chat thread**.

Canary was five percent on the server. The UI still behaved like twenty. Support argued with graphs that could not agree.

Someone said the sentence that costs a night:

**“A canary that only one tier sees is not a canary. It is a split brain.”**

That sentence is the brief for Kiponos when you stop treating languages as silos and start treating them as **peers on one Team profile**.

I have always believed people should not have to ship a release to make a decision. With four SDKs on **one living tree**, the decision is a leaf. The session stays. The browser **mirrors** truth from a process that holds identity — it never becomes a vault for Connect tokens.

---

## What “same Team” actually means

| Role | Where it runs | How it joins the Team |
|------|---------------|------------------------|
| **Server (Java)** | JVM / workers | `Kiponos.createForCurrentTeam()` |
| **Server / agent (Python)** | agents, workers | connect once; read/write live leaves |
| **Web BFF (React Node)** | your Node process | `@kiponos/react` **server** peer |
| **Admin / ops (Angular Node)** | your Node process | `@kiponos/angular` **server** peer |
| **Users / operators (browser)** | SPA / admin UI | **mirror** via your API/SSE — **no** hub tokens in JS |

Same `KIPONOS` profile. Same tree. Same second the leaf moves.

| Old habit | Cost |
|-----------|------|
| Flag per language repo | Four PRs for one truth |
| Tokens in the SPA | Security incident waiting to happen |
| Restart agent to flip posture | Lost context mid-run |
| Admin wall from a different source | War-room lies |

<!-- medium-img: diagram-before-after.png -->

---

## The Super Pattern (one leaf, four voices)

Hub leaf for this story:

```text
release/canary-percent = 5
```

**Java** (server peer):

```java
Kiponos k = Kiponos.createForCurrentTeam();
try {
    // ensure + read release/canary-percent
    // honor live fullstack canary percent
} finally {
    k.disconnect();
}
```

**Python** (agent peer — session stays up):

```python
# connect once; flip the leaf without killing the process
from kiponos import Kiponos
k = Kiponos.connect(quiet=True)
print("python peer on the same Team tree")
```

**React Node** (web/BFF identity):

```ts
import { Kiponos } from '@kiponos/react/server';
const kip = Kiponos.createFromEnv();
await kip.connect();
// set/get the same leaf — browser only sees your mirror
```

**Angular Node** (admin identity):

```ts
import { Kiponos } from '@kiponos/angular/server';
const kip = Kiponos.createFromEnv();
await kip.connect();
// same Team profile, same leaf as Java and React
```

When anyone — dashboard, on-call, agent, or peer — changes the leaf, **every honest peer** moves. That is the magic: not four clients, but **one Team collaborating in real time**.

<!-- medium-img: diagram-flow.png -->

---

## The runnable example

| Peer | Path |
|------|------|
| Java | `examples/java/canary-fullstack-live` |
| Python | `examples/python/canary-fullstack-live` |
| Node React | `examples/node/canary-fullstack-live-react` |
| Node Angular | `examples/node/canary-fullstack-live-angular` |
| Story | `docs/examples/medium-drafts/canary-fullstack-live.md` |

Public: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/canary-fullstack-live) · hub docs [kiponos.io](https://kiponos.io)

Do **not** publish private team homes as demo URLs.

---

## How to try (end-to-end)

```bash
export KIPONOS_ID=… KIPONOS_ACCESS=…
export KIPONOS="['MyApp']['1.0']['Dev']['base']"   # same Team profile everywhere

# 1) React Node writes the leaf
cd examples/node/canary-fullstack-live-react && npm install && node peer.mjs 5

# 2) Angular Node can write or confirm the same leaf
cd examples/node/canary-fullstack-live-angular && npm install && node peer.mjs 5

# 3) Java reads it without restart folklore
cd examples/java/canary-fullstack-live && ./gradlew test && ./gradlew run

# 4) Python peer (agent stays alive)
cd examples/python/canary-fullstack-live && python3 peer.py
```

Two tabs. Four peers. One Team. One leaf.

## Old world vs same-Team hub

| Move | Old world | Same Team tree |
|------|-----------|----------------|
| Change fullstack canary percent | Redeploy each language | One `set` on the hub |
| Browser role | Fake hub client / leaked tokens | Mirror of **your** BFF |
| War room | Three screens, three truths | One leaf, every peer |
| Agent | Restart to flip skill/gate | Session stays; leaf moves |

---

## Guardrails

1. Process identity only — never Connect tokens in SPA bundles.  
2. One profile tree for Java, Python, React Node, Angular Node.  
3. Browser = mirror (SSE/API you control).  
4. Fail closed on dangerous paths when the leaf is missing.  
5. Measure **seconds from judgment to every peer**, not “we have four SDKs.”  
6. Never publish private path trees in public articles.

| Do | Don't |
|----|-------|
| Hold tokens on server processes | Embed tokens in Web bundles |
| Share one Team profile | Per-language folklore flags |
| Prove multi-peer in two tabs | Claim “fullstack” with one README |

---

## The moral

**People should not have to ship a release to make a decision** — and Web, Server, and Users should not need three different truths to collaborate.

The product magic is simple to say and rare to build: **one Team tree, many peers, real time.**

Pain we retire: *canaries that only flip one tier while clients keep old behavior*.

---

## The lie we stop telling

“We’ll sync the frontend after the backend deploy.”

No. We’ll put **fullstack canary percent** on the hub, put identity on every process peer, and let the browser mirror the Team — so collaboration is a leaf change, not a release train.

---

*Example: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/canary-fullstack-live](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/canary-fullstack-live)*
