# Canary Percent Live on the Hub (React SDK)

*A traveler's note on traffic split percent — with the Kiponos React SDK as a hub peer.*

---

I have heard an on-call engineer say they needed five percent of traffic, not a full release train.

That is the class of decision that is **too small for a release** and **too important for a chat thread**. Kiponos exists so a leaf like `release/canary-percent` can move while every peer — dashboard, Java, Python, and now **@kiponos/react** — stays honest.

I have always believed people should not have to ship a release to make a decision. This story is one concrete use case.

<!-- medium-img: diagram-before-after.png -->

---

## What went wrong (the human version)

Someone needed to change **traffic split percent**. The path of least resistance was a config file in a deployable artifact. On-call waited. Product waited. The pager did not care about your green pipeline.

The Super Pattern is simpler: put the leaf on the hub, let peers read locally after bootstrap, and push deltas when anyone sets a value.

## The Super Pattern

Hub path: `release/canary-percent` (example default `5`)

```ts
import { Kiponos } from '@kiponos/react/server';

const kip = Kiponos.createFromEnv();
await kip.connect();
await kip.ensurePath('release');
await kip.path('release').set('canary-percent', '5');
// live UI (BFF-injected client):
// const v = useKiponosValue('release/canary-percent', { defaultValue: '5' });
```

Java peer (same profile):

```java
Kiponos kip = Kiponos.createForCurrentTeam();
String v = kip.path("release").get("canary-percent", "5");
// or get at root segments matching the tree
```

Public package tree: `sdks/kiponos-react-sdk`  
Companion example: `examples/java/react-sdk-canary-percent`

<!-- medium-img: diagram-flow.png -->

---

## The example pattern

1. Connect with process env (`KIPONOS_ID` / `KIPONOS_ACCESS` / `KIPONOS`) — never browser secrets.  
2. `ensurePath` / `path(...).set` for `release/canary-percent`.  
3. Java `createForCurrentTeam()` reads the same leaf; `afterValueUpdated` fires on change.  
4. Dashboard shows the value without refresh.

How to try: open `sdks/kiponos-react-sdk`, run unit tests, then wire `createFromEnv` in a Node BFF. Companion: `examples/java/react-sdk-canary-percent`.

---

## Guardrails

| Do | Don't |
|----|-------|
| Hold tokens in Node/Java process env | Embed tokens in SPA bundles |
| Use one profile tree for all peers | Invent a second source of truth |
| Treat `get` as local after bootstrap | Poll REST for every render |

---

## The moral

People should not have to ship a release to make a decision.

**Canary Percent Live on the Hub** is not a tutorial for its own sake — it is proof that the React SDK is a **hub peer**, not a glorified fetch wrapper. Change `release/canary-percent`. Watch the tree. Keep the release train for real code.

That is the whole moral.
