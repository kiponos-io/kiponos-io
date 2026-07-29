# Incident Pause Flag the Agents Obey (Angular SDK)

*A traveler's note on pause flag for agents — with the Kiponos Angular SDK as a hub peer.*

---

I have drawn a line under automated remediation the moment a human said stop.

That is the class of decision that is **too small for a release** and **too important for a chat thread**. Kiponos exists so a leaf like `ops/incident/paused` can move while every peer — dashboard, Java, Python, and now **@kiponos/angular** — stays honest.

I have always believed people should not have to ship a release to make a decision. This story is one concrete use case.

<!-- medium-img: diagram-before-after.png -->

---

## What went wrong (the human version)

Someone needed to change **pause flag for agents**. The path of least resistance was a config file in a deployable artifact. On-call waited. Product waited. The pager did not care about your green pipeline.

The Super Pattern is simpler: put the leaf on the hub, let peers read locally after bootstrap, and push deltas when anyone sets a value.

## The Super Pattern

Hub path: `ops/incident/paused` (example default `no`)

```ts
import { Kiponos, injectKiponos, provideKiponos } from '@kiponos/angular';

// Node BFF
const client = Kiponos.createFromEnv();
await client.connect();
await client.path('ops', 'incident').set('paused', 'no');

// Angular component (client injected via provideKiponos)
// readonly v = injectKiponos().value('ops/incident/paused', { defaultValue: 'no' });
```

Java peer (same profile):

```java
Kiponos kip = Kiponos.createForCurrentTeam();
String v = kip.path("ops", "incident").get("paused", "no");
// or get at root segments matching the tree
```

Public package tree: `sdks/kiponos-angular-sdk`  
Companion example: `examples/java/angular-sdk-incident-pause`

<!-- medium-img: diagram-flow.png -->

---

## Install (npm)

```bash
npm install @kiponos/angular
```

Public package: https://www.npmjs.com/package/@kiponos/angular  
Runnable Node example: `examples/node/angular-status-wall`  
Source: https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-angular-sdk

## The example pattern

1. Connect with process env (`KIPONOS_ID` / `KIPONOS_ACCESS` / `KIPONOS`) — never browser secrets.  
2. `ensurePath` / `path(...).set` for `ops/incident/paused`.  
3. Java `createForCurrentTeam()` reads the same leaf; `afterValueUpdated` fires on change.  
4. Dashboard shows the value without refresh.

How to try: use `npm install @kiponos/angular` and `examples/node/angular-status-wall`

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

**Incident Pause Flag the Agents Obey** is not a tutorial for its own sake — it is proof that the Angular SDK is a **hub peer**, not a glorified fetch wrapper. Change `ops/incident/paused`. Watch the tree. Keep the release train for real code.

That is the whole moral.
