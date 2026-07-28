# The Fourth Voice on the Hub Was Not Another JVM

*A traveler’s note from a living config tree.*

---

There is a class of production decisions that are **too small for a release** and **too important for a chat thread**.

We already had three kinds of participants on the Kiponos hub:

1. Humans on the **dashboard**  
2. **Java** services with `createForCurrentTeam()`  
3. **Python** agents writing ops state  

What we did **not** have was a first-class **Node/React backend** peer with the same contract: env identity, in-memory tree, live deltas.

Redeploying a frontend bundle to change `demo/status-wall/status-alpha` is how teams invent folklore.

---

## The jar was fine. The browser was the wrong place for secrets.

A SaaS “React app” is usually two things people mash into one word:

| Piece | Runs where | Holds Connect tokens? |
|-------|------------|------------------------|
| SPA bundle | Visitor’s browser | **Never** |
| Node (or any) API | Your machine / cluster | **Yes — like Java** |

`.env` on a build host is not “safe” if the bundler inlines `VITE_KIPONOS_ID` into public JS.

So the React SDK is honest:

```js
import { Kiponos } from '@kiponos/react/server';

const kip = Kiponos.createFromEnv(); // or createForCurrentTeam()
await kip.connect();
```

No constructor tokens. Process env only — the same idea as Java’s singleton.

---

## Hub tree

```text
demo/status-wall/status-alpha = online
demo/status-wall/status-beta  = home
demo/status-wall/note         = live
demo/status-wall/last-ping    = <iso>
```

Local `get()` on the hot path. Dashboard, Java, Python, or Node `set()` when the world changes.

<!-- medium-img: diagram-hub-peers.png -->

---

## Snippet (Java still peers with Node)

```java
Kiponos k = Kiponos.createForCurrentTeam();
Folder wall = k.path("demo", "status-wall");
String alpha = wall.get("status-alpha", "—");
// Node createFromEnv wrote this seconds ago — no redeploy
```

Node side (the new participant):

```js
const kip = Kiponos.createForCurrentTeam();
await kip.connect();
await kip.path("demo", "status-wall").set("status-alpha", "focus");
```

The browser UI talks **SSE to your Node process**, not Connect tokens to the hub.

---

## Live proof

Run the pattern locally: a small status-wall SPA talks to your Node process (`createFromEnv` + SSE), and a Java peer on the same hub profile reads the same leaves.

Two tabs. One status flip. Everyone sees it — including a JVM that never restarted.

Public surface: **[kiponos.io](https://kiponos.io)** (hub + docs) and the example tree on GitHub.  
Do **not** treat private team homes / family ops walls as public demo URLs.

---

## How to try

```bash
export KIPONOS_ID=… KIPONOS_ACCESS=…
export KIPONOS="['MyApp']['1.0']['Dev']['base']"

# Node
import { Kiponos } from '@kiponos/react/server'
const kip = Kiponos.createFromEnv()
await kip.connect()
```

Java remains:

```java
Kiponos.createForCurrentTeam();
```

Same hub. Same moral.

---

## The moral

**People should not have to ship a release to make a decision** — and they should not paste service tokens into a SPA to share state.  

Identity is **where the process runs**. The fourth voice is Node. The UI can stay thin, live, and honest.


---

## Configuration hell, restated for frontends

Backend teams already learned: packaging timeouts in a jar turns every incident into a release. Frontend teams are replaying the same movie with a different costume — shipping a new SPA build to flip a shared operational string.

The hub does not care whether the writer was Spring Boot, a Python agent, or a Node process that happens to serve a React tree. It cares that **identity is a process**, and that deltas arrive on a living connection.

---

## What went wrong in the usual SPA story

1. Secrets were treated as “env” because the filename was `.env`.  
2. The bundler inlined Connect tokens into public JS.  
3. Browser WebSocket upgrades could not carry Java-style handshake headers.  
4. Someone proposed weakening the server handshake “for CORS” — which was never the diagnosis.

The correct story is duller and safer: the **Node API** is the participant; the SPA is a client of *your* API.

---

## The example pattern

A status wall is intentionally small:

- A few shared status fields  
- A shared note  
- A ping timestamp  

Enough to prove bi-directional updates without building a second product. Keep **private** ops homes (family tools, commute walls, OTP timing) off public write-ups.

When Java calls:

```java
k.path("demo", "status-wall").get("status-alpha", "—");
```

…it is reading the same leaf the Node `createFromEnv` process wrote after a button press in the SPA.

---

## A traveler’s checklist

- Prefer `createForCurrentTeam` / `createFromEnv` over token constructors.  
- Keep Connect tokens next to other production secrets.  
- Bridge browsers with SSE/API you control.  
- Let the dashboard remain the human surface; let SDKs remain service surfaces.  
- Measure success by **seconds from judgment to effect**, not by how many languages have a client library.  
- Never publish private team path trees or private product URLs in public articles.

---

## The lie we stop telling

“We’ll just put the SDK in the frontend.”  

No. We’ll put **live state** in the hub, put **identity** on the process, and let the frontend stay a fast mirror.
