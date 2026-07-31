# Live Theme Toggle Without a SPA Redeploy (React SDK)

*A traveler’s note on a theme string that should never wait for a frontend train — with the Kiponos React SDK as a hub peer.*

---

There is a class of production decisions that are **too small for a release** and **too important for a chat thread**.

I have watched a design lead wait for a SPA deploy to flip dark mode for a customer demo. The backend already knew the brand colors. The JVM already knew the tenant. The only thing missing was a leaf that every peer could read **now** — dashboard, Java, Python, and a Node process holding **@kiponos/react** identity — without asking the release train for permission.

Redeploying a frontend bundle to change `ui/theme` is how teams invent folklore. Someone ships a webpack graph to flip a shared string. Tokens drift into public JS because the file was named `.env`. The browser becomes a participant it was never meant to be.

That is not real-time config. That is a ceremony with a theme toggle attached.

I have always believed people should not have to ship a release to make a decision. This story is one concrete use case.

---

## What went wrong (the human version)

A SaaS “React app” is usually two things people mash into one word:

| Piece | Runs where | Holds Connect tokens? |
|-------|------------|------------------------|
| SPA bundle | Visitor’s browser | **Never** |
| Node (or any) API / BFF | Your machine / cluster | **Yes — like Java** |

The engineers were not stupid. The UI needed a live theme. The mistake was treating the **browser** as the hub participant — baking defaults into the build, or worse, wiring SDK secrets into a client bundle.

I once heard a lead say, half joking, half broken:

**“If we could flip `ui/theme` the way we flip a kill switch on the hub, I’d stop shipping SPA builds for demos.”**

That sentence is the whole product brief.

<!-- medium-img: diagram-before-after.png -->

---

## The Super Pattern (process identity, not a SPA secret)

The React SDK is honest about where identity lives:

```ts
import { Kiponos } from '@kiponos/react/server';

const kip = Kiponos.createFromEnv(); // or createForCurrentTeam()
await kip.connect();
await kip.ensurePath('ui');
await kip.path('ui').set('theme', 'dark');
// live UI (BFF-injected client mirror):
// const v = useKiponosValue('ui/theme', { defaultValue: 'dark' });
```

No constructor tokens in the browser. Process env only — the same idea as Java’s singleton.

Hub tree (example):

```text
ui / theme = dark
ui / density = comfortable
ui / last-set-by = design-lead
ui / last-set-at = <iso>
```

Local `get()` on the hot path after bootstrap. Dashboard, Java, Python, or Node `set()` when judgment arrives. The SPA talks **SSE or API to your Node process**, not Connect tokens to the hub.

That is the Super Pattern for frontends:

> Live hub + process identity + thin UI mirror = decisions that move without a release **or** a leaked token.

<!-- medium-img: diagram-flow.png -->

---

## The example (Java still peers with Node)

Published under **`examples/java/react-sdk-live-theme`** on [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/react-sdk-live-theme)

The Java peer reads the same leaf the Node process wrote:

```java
Kiponos k = Kiponos.createForCurrentTeam();
try {
    String theme = k.path("ui").get("theme", "dark");
    System.out.println("ui/theme=" + theme);
    // Node createFromEnv wrote this seconds ago — no redeploy
} finally {
    k.disconnect();
}
```

Node side (the voice that owns identity):

```ts
const kip = Kiponos.createFromEnv();
await kip.connect();
await kip.ensurePath("ui");
await kip.path("ui").set("theme", "light");
await kip.path("ui").set("last-set-at", new Date().toISOString());
```

Two tabs. One theme flip. Everyone sees it — including a JVM that never restarted, and a design lead who never opened a PR.

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
#    createFromEnv → connect → path('ui').set('theme', 'dark')

# 4) Optional Java peer on the same tree
#    examples/java/react-sdk-live-theme
```

Package: [@kiponos/react on npm](https://www.npmjs.com/package/@kiponos/react)

## Old world vs live hub

| Move | Old world (SPA as participant) | Live hub (Node peer) |
|------|--------------------------------|----------------------|
| Flip theme for a demo | Ship SPA build | Node / dashboard `set()` |
| Where tokens live | Often public JS | Process env only |
| Java / other peers see it | After redeploy folklore | Same tree, live deltas |
| Browser role | Fake hub client | Client of **your** API / SSE |
| Failure mode | “Which build is dark mode?” | One leaf, one truth |

---

## Guardrails (traveler’s checklist)

1. Prefer `createForCurrentTeam` / `createFromEnv` over token constructors.  
2. Keep Connect tokens next to other production secrets — never in a bundler input that ships to visitors.  
3. Bridge browsers with SSE/API you control; inject theme as a **mirror**, not as a second source of truth.  
4. Let the dashboard remain the human surface; let SDKs remain service surfaces.  
5. Measure success by **seconds from judgment to effect**, not by how many languages have a client library.  
6. Never publish private team path trees or private product URLs in public articles.  
7. Rehearse a two-tab proof (Node write → Java read → UI mirror) before the next “just put theme in the frontend config” meeting.

| Do | Don't |
|----|-------|
| Hold tokens in Node/Java process env | Embed tokens in SPA bundles |
| Use one profile tree for all peers | Invent a second source of truth for theme |
| Treat `get` as local after bootstrap | Poll REST for every paint |
| Flip `ui/theme` from dashboard in an incident | Wait for green pipeline to undo dark mode |

---

## The moral

**People should not have to ship a release to make a decision** — and they should not paste service tokens into a SPA to share a theme string.

Identity is **where the process runs**. The React SDK is a **hub peer**, not a glorified fetch wrapper. Change `ui/theme`. Watch the tree. Keep the release train for real code.

Ship the peer once. Leave the bundle alone when the only thing that changed is a shared operational string.

---

## The lie we stop telling

“We’ll just bake the theme into the next frontend release.”

No. We’ll put **live state** in the hub, put **identity** on the process, and let the frontend stay a fast mirror of `ui/theme`.

---

*Example + tree: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/react-sdk-live-theme](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/react-sdk-live-theme)*
