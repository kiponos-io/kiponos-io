# A/B Weights on Angular Without a Client Bundle Ship

*A traveler’s note on Angular experiment weights — with the Kiponos Angular SDK as a hub peer, plus Java and Python on the same living tree.*

---

There is a class of production decisions that are **too small for a release** and **too important for a chat thread**.

Product wanted to cut variant B for admin users only. The weights lived in an Angular environment file. Engineering queued a release.

Someone said the sentence that always costs a night:

**“Experiment control that waits for a SPA train is not control. It is scheduling.”**

That sentence is the product brief.

I have always believed people should not have to ship a release to make a decision. This story is one concrete use case — **Java**, **Python**, and **@kiponos/angular** (Node server peer) on one leaf: `experiments/ab-weights`.

Redeploying a frontend, restarting a BFF, or bouncing an agent just to move **Angular experiment weights** is how teams invent folklore. Tokens drift into public bundles. Agents lose session memory. Java keeps the old value until the next ConfigMap. The decision was simple; the ceremony was not.

---

## What went wrong (the human version)

Angular apps are usually two things people mash into one word:

| Piece | Runs where | Holds Connect tokens? |
|-------|------------|------------------------|
| SPA / admin bundle | Visitor or operator browser | **Never** |
| Node (or any) BFF / peer | Your machine / cluster | **Yes — like Java** |

The engineers were not stupid. They needed **Angular experiment weights** to move. The mistake was freezing it in a build-time env, a YAML file, or a process restart — or treating the **browser** as the hub participant.

| Old habit | What it costs |
|-----------|----------------|
| Flag in SPA env | Minutes to hours of train delay |
| Restart Node to re-read env | Dropped sessions mid-incident |
| Separate Java ConfigMap | Peers disagree; war-room confusion |
| Restart agent to flip posture | Lost context, lost time |

<!-- medium-img: diagram-before-after.png -->

---

## The Super Pattern (process identity, multi-peer)

Hub leaf (this example):

```text
experiments/ab-weights = 70,30
```

**@kiponos/angular** server peer (identity on the process):

```ts
import { Kiponos } from '@kiponos/angular/server';

const kip = Kiponos.createFromEnv();
await kip.connect();
await kip.ensurePath('experiments');
await kip.path('experiments').set('ab-weights', '70,30');
// UI mirror via your BFF/SSE — never Connect tokens in the browser
// const v = useKiponosValue('experiments/ab-weights', { defaultValue: '70,30' });
```

Java peer (same profile tree):

```java
Kiponos k = Kiponos.createForCurrentTeam();
try {
    Folder p = k.getRootFolder().folderOrCreate("experiments");
    String v = p.hasKey("ab-weights") ? p.get("ab-weights") : "70,30";
    // honor live Angular experiment weights
    System.out.println("ab-weights=" + v);
} finally {
    k.disconnect();
}
```

Python agent peer (session stays up):

```python
# connect once; flip the leaf without killing the agent
from kiponos import Kiponos
k = Kiponos.connect(quiet=True)
# hub leaf: experiments/ab-weights
print("python peer online — change ab-weights on the hub, keep this process")

```

No constructor tokens in the browser. Process env only — the same idea as Java’s singleton. The SPA or Angular admin talks **SSE or API to your Node process**, not Connect tokens to the hub. When the leaf moves, every honest peer honors it — without a jar, without a SPA ship, without killing the agent.

That is the Super Pattern:

> Live hub + process identity + thin UI mirror = operational posture in seconds, not deploy minutes.

<!-- medium-img: diagram-flow.png -->

---

## The example (runnable multi-SDK tree)

Published under:

| Peer | Path |
|------|------|
| Java | `examples/java/angular-sdk-ab-weights` |
| Python | `examples/python/angular-sdk-ab-weights` |
| Node (angular) | `examples/node/angular-sdk-ab-weights` |
| Story | `docs/examples/medium-drafts/angular-sdk-ab-weights.md` |

Public surface: **[kiponos.io](https://kiponos.io)** and [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/angular-sdk-ab-weights).

Do **not** treat private team homes or private ops walls as public demo URLs.

---

## Install (angular / npm)

```bash
npm install @kiponos/angular
```

Public package: https://www.npmjs.com/package/@kiponos/angular  
SDK source tree on the public repo under `sdks/`.

## How to try

```bash
# 1) Credentials (same as Java)
export KIPONOS_ID=… KIPONOS_ACCESS=…
export KIPONOS="['MyApp']['1.0']['Dev']['base']"

# 2) Node peer writes the leaf
cd examples/node/angular-sdk-ab-weights
npm install && node peer.mjs 70,30

# 3) Java peer reads the same leaf
cd examples/java/angular-sdk-ab-weights
./gradlew test
./gradlew run

# 4) Python peer (pure logic + optional live)
cd examples/python/angular-sdk-ab-weights
python3 peer.py
```

Two tabs. One leaf. Everyone sees it — including a JVM that never restarted, a Python agent that kept its session, and a UI that mirrors your BFF.

## Old world vs live hub

| Move | Old world | Live hub |
|------|-----------|----------|
| Change Angular experiment weights | Ship SPA / restart BFF | Node or dashboard `set` |
| Where tokens live | Often public JS | Process env only |
| Java / Python peers | After redeploy folklore | Same tree, live deltas |
| Browser role | Fake hub client | Client of **your** API / SSE |
| Failure mode | Stale until next train | One leaf, fail-closed peers |

---

## Guardrails (traveler’s checklist)

1. Prefer `createForCurrentTeam` / `createFromEnv` over token constructors.  
2. Keep Connect tokens next to other production secrets — never in a bundler input that ships to visitors.  
3. Bridge browsers with SSE/API you control; treat the leaf as **ops posture**, not as a toy alone.  
4. Fail closed when the leaf is missing if the path is dangerous.  
5. Measure success by **seconds from judgment to effect**, not by how many languages have a client library.  
6. Never publish private team path trees or private product URLs in public articles.  
7. Rehearse a multi-peer proof (Node set → Java read → Python honor → UI mirror) before the next fire.

| Do | Don't |
|----|-------|
| Hold tokens in Node/Java/Python process env | Embed tokens in SPA bundles |
| Use one profile tree for all peers | Second “frontend-only” source of truth |
| Treat `get` as local after bootstrap | Poll REST for every request |
| Move the leaf mid-incident | Wait for green pipeline to stop bleeding |

---

## The moral

**People should not have to ship a release to make a decision** — and they should not paste service tokens into a SPA to share **Angular experiment weights**.

Identity is **where the process runs**. The Angular SDK is a **hub peer**, not a glorified fetch wrapper. Change `experiments/ab-weights`. Watch the tree. Keep the release train for real code.

Ship the peers once. Leave the bundle alone when the only thing that changed is a shared operational leaf.

---

## The lie we stop telling

“We’ll fix Angular experiment weights in the next angular release.”

No. We’ll put **live posture** in the hub, put **identity** on the process, and let every peer — Java, Python, Node, thin UI mirror — honor `experiments/ab-weights` without waiting for CI.

Pain we retire: *Angular experiment weights ship only with the SPA*.

---

*Example tree: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/angular-sdk-ab-weights](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/angular-sdk-ab-weights)*
