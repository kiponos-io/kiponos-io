# Feature Kill Switch From the Hub (React SDK)

*A traveler’s note on a boolean kill flag that should never wait for a frontend train — with the Kiponos React SDK as a hub peer.*

---

There is a class of production decisions that are **too small for a release** and **too important for a chat thread**.

I have sat through a war room where the only fix was a redeploy to turn a feature off. Error budget was burning. Support was filling the queue. Someone had already typed the word “off” in three chat threads. The missing piece was a leaf every peer could honor **now** — dashboard, Java, Python, and a Node process holding **@kiponos/react** identity — without asking the release train for a green pipeline.

Redeploying a frontend (or a monorepo “web” package) to flip `flags/feature-x` is how teams invent folklore. Someone ships a build to kill a path. Tokens drift into public JS because someone “needed the SDK in the browser.” The feature stays on until CI finishes arguing with itself.

That is not incident posture. That is a ceremony with a boolean attached.

I have always believed people should not have to ship a release to make a decision. This story is one concrete use case.

---

## What went wrong (the human version)

A SaaS “React app” is usually two things people mash into one word:

| Piece | Runs where | Holds Connect tokens? |
|-------|------------|------------------------|
| SPA bundle | Visitor’s browser | **Never** |
| Node (or any) API / BFF | Your machine / cluster | **Yes — like Java** |

The engineers were not stupid. The UI needed a killable path. The mistake was treating the **browser** as the hub participant — or freezing the flag in a build-time env that only changes when the train moves.

I once heard an on-call lead say, not joking at all:

**“If we could kill this path from the hub the way we kill a payment processor, I’d stop shipping SPA builds at 2 a.m.”**

That sentence is the whole product brief.

<!-- medium-img: diagram-before-after.png -->

---

## The Super Pattern (process identity, not a SPA secret)

The React SDK is honest about where identity lives:

```ts
import { Kiponos } from '@kiponos/react/server';

const kip = Kiponos.createFromEnv(); // or createForCurrentTeam()
await kip.connect();
await kip.ensurePath('flags');
await kip.path('flags').set('feature-x', 'off');
// live UI (BFF-injected client mirror):
// const v = useKiponosValue('flags/feature-x', { defaultValue: 'off' });
```

No constructor tokens in the browser. Process env only — the same idea as Java’s singleton.

Hub tree (example):

```text
flags / feature-x = off
flags / feature-x-reason = error-budget
flags / feature-x-set-by = oncall
flags / feature-x-set-at = <iso>
```

Local `get()` on the hot path after bootstrap. Dashboard, Java, Python, or Node `set()` when judgment arrives. The SPA talks **SSE or API to your Node process**, not Connect tokens to the hub. When the leaf goes `off`, every honest peer stops the path — without a jar, without a SPA ship.

That is the Super Pattern for frontend-adjacent kills:

> Live hub + process identity + thin UI mirror = incident posture in seconds, not in deploy minutes.

<!-- medium-img: diagram-flow.png -->

---

## The example (Java still peers with Node)

Published under **`examples/java/react-sdk-feature-kill`** on [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/react-sdk-feature-kill)

The Java peer reads the same leaf the Node process wrote:

```java
Kiponos k = Kiponos.createForCurrentTeam();
try {
    String flag = k.path("flags").get("feature-x", "off");
    if (!"on".equalsIgnoreCase(flag)) {
        // fail closed: path killed without a redeploy
        return;
    }
    // serve the feature path
} finally {
    k.disconnect();
}
```

Node side (the voice that owns identity and often the BFF):

```ts
const kip = Kiponos.createFromEnv();
await kip.connect();
await kip.ensurePath("flags");
await kip.path("flags").set("feature-x", "off");
await kip.path("flags").set("feature-x-set-at", new Date().toISOString());
```

Two tabs. One kill. Everyone sees it — including a JVM that never restarted, and a UI that stops offering the path because the BFF mirror went dark.

Public surface: **[kiponos.io](https://kiponos.io)** (hub + docs) and the example tree on GitHub.  
Do **not** treat private team homes or private ops walls as public demo URLs.

---

## Install (npm)

```bash
npm install @kiponos/react
```

Public package: https://www.npmjs.com/package/@kiponos/react  
Runnable Node peer pattern: `examples/node/react-status-wall`  
SDK source: https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-react-sdk

## How to try

```bash
# 1) Install the SDK from npm
npm install @kiponos/react

# 2) Connect credentials (same as Java)
export KIPONOS_ID=… KIPONOS_ACCESS=…
export KIPONOS="['MyApp']['1.0']['Dev']['base']"

# 3) From a Node process (never the browser)
#    createFromEnv → connect → path('flags').set('feature-x', 'off')

# 4) Optional Java peer on the same tree
#    examples/java/react-sdk-feature-kill
```

Package: [@kiponos/react on npm](https://www.npmjs.com/package/@kiponos/react)

## Old world vs live hub

| Move | Old world (flag in a build) | Live hub (Node peer) |
|------|-----------------------------|----------------------|
| Kill a feature path | Ship SPA / BFF build | Node / dashboard `set('off')` |
| Where tokens live | Often public JS | Process env only |
| Java / other peers see it | After redeploy folklore | Same tree, live deltas |
| Browser role | Fake hub client | Client of **your** API / SSE |
| Failure mode | “Still on until next deploy” | One leaf, fail-closed peers |

---

## Guardrails (traveler’s checklist)

1. Prefer `createForCurrentTeam` / `createFromEnv` over token constructors.  
2. Keep Connect tokens next to other production secrets — never in a bundler input that ships to visitors.  
3. Bridge browsers with SSE/API you control; treat the flag as **incident posture**, not as a product experiment toy alone.  
4. Fail closed when the leaf is missing or not `"on"` if the path is dangerous.  
5. Measure success by **seconds from judgment to effect**, not by how many languages have a client library.  
6. Never publish private team path trees or private product URLs in public articles.  
7. Rehearse a two-tab proof (Node kill → Java respects → UI mirror hides path) before the next 2 a.m. feature fire.

| Do | Don't |
|----|-------|
| Hold tokens in Node/Java process env | Embed tokens in SPA bundles |
| Use one profile tree for all peers | Second “frontend-only” flag source |
| Treat `get` as local after bootstrap | Poll REST for every request |
| Kill from dashboard in an incident | Wait for green pipeline to stop bleeding |

---

## The moral

**People should not have to ship a release to make a decision** — and they should not paste service tokens into a SPA to share a kill flag.

Identity is **where the process runs**. The React SDK is a **hub peer**, not a glorified fetch wrapper. Change `flags/feature-x`. Watch the tree. Keep the release train for real code.

Ship the peer once. Leave the bundle alone when the only thing that changed is a shared operational boolean.

---

## The lie we stop telling

“We’ll just turn it off in the next frontend release.”

No. We’ll put **live posture** in the hub, put **identity** on the process, and let every peer — including a thin UI mirror — honor `flags/feature-x` without waiting for CI.

---

*Example + tree: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/react-sdk-feature-kill](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/react-sdk-feature-kill)*
