# Live Theme Toggle Without a SPA Redeploy (Angular SDK)

*A traveler's note on theme string on the hub — with the Kiponos Angular SDK as a hub peer.*

---

I have watched a design lead wait for a frontend deploy to flip dark mode for a demo.

That is the class of decision that is **too small for a release** and **too important for a chat thread**. Kiponos exists so a leaf like `ui/theme` can move while every peer — dashboard, Java, Python, and now **@kiponos/angular** — stays honest.

I have always believed people should not have to ship a release to make a decision. This story is one concrete use case.

<!-- medium-img: diagram-before-after.png -->

---

## What went wrong (the human version)

Someone needed to change **theme string on the hub**. The path of least resistance was a config file in a deployable artifact. On-call waited. Product waited. The pager did not care about your green pipeline.

The Super Pattern is simpler: put the leaf on the hub, let peers read locally after bootstrap, and push deltas when anyone sets a value.

## The Super Pattern

Hub path: `ui/theme` (example default `dark`)

```ts
import { Kiponos, injectKiponos, provideKiponos } from '@kiponos/angular';

// Node BFF
const client = Kiponos.createFromEnv();
await client.connect();
await client.path('ui').set('theme', 'dark');

// Angular component (client injected via provideKiponos)
// readonly v = injectKiponos().value('ui/theme', { defaultValue: 'dark' });
```

Java peer (same profile):

```java
Kiponos kip = Kiponos.createForCurrentTeam();
String v = kip.path("ui").get("theme", "dark");
// or get at root segments matching the tree
```

Public package tree: `sdks/kiponos-angular-sdk`  
Companion example: `examples/java/angular-sdk-live-theme`

<!-- medium-img: diagram-flow.png -->

---

## The example pattern

1. Connect with process env (`KIPONOS_ID` / `KIPONOS_ACCESS` / `KIPONOS`) — never browser secrets.  
2. `ensurePath` / `path(...).set` for `ui/theme`.  
3. Java `createForCurrentTeam()` reads the same leaf; `afterValueUpdated` fires on change.  
4. Dashboard shows the value without refresh.

How to try: open `sdks/kiponos-angular-sdk`, `npm test`, then `provideKiponos({{ client }})` from a server-created client. Companion: `examples/java/angular-sdk-live-theme`.

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

**Live Theme Toggle Without a SPA Redeploy** is not a tutorial for its own sake — it is proof that the Angular SDK is a **hub peer**, not a glorified fetch wrapper. Change `ui/theme`. Watch the tree. Keep the release train for real code.

That is the whole moral.
