# The Fourth Voice on the Hub Was Not Another JVM

*A traveler’s note from a living config tree.*

---

There is a class of production decisions that are **too small for a release** and **too important for a chat thread**.

We already had three kinds of participants on the Kiponos hub:

1. Humans on the **dashboard**  
2. **Java** services with `createForCurrentTeam()`  
3. **Python** agents writing ops state  

What we did **not** have was a first-class **Node/React backend** peer with the same contract: env identity, in-memory tree, live deltas.

Redeploying a frontend bundle to change `family/mirror-phone/status-moshe` is how teams invent folklore.

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
family/mirror-phone/status-moshe = online
family/mirror-phone/status-mush  = home
family/mirror-phone/note         = live
family/mirror-phone/last-ping    = <iso>
```

Local `get()` on the hot path. Dashboard, Java, Python, or Node `set()` when the world changes.

<!-- medium-img: diagram-hub-peers.png -->

---

## Snippet (Java still peers with Node)

```java
Kiponos k = Kiponos.createForCurrentTeam();
Folder wall = k.path("family", "mirror-phone");
String moshe = wall.get("status-moshe", "—");
// Node createFromEnv wrote this seconds ago — no redeploy
```

Node side (the new participant):

```js
const kip = Kiponos.createForCurrentTeam();
await kip.connect();
await kip.path("family", "mirror-phone").set("status-moshe", "focus");
```

The browser UI talks **SSE to your Node process**, not Connect tokens to the hub.

---

## Live proof

**[Mirror Phone](https://kiponos.io/mirror/)** on the same profile as the family ops tree.  
Linked from **[Operator](https://kiponos.io/operator/)**.

Two tabs. One status flip. Everyone sees it — including a JVM that never restarted.

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
