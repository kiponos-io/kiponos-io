# The Fifth Voice on the Hub Was Angular — And It Refused SPA Secrets

*A traveler’s note from a living config tree: Signals, DI, and the same contract as Java.*

---

I have sat in war rooms where the Kiponos hub already had four honest participants:

1. Humans on the **dashboard**  
2. **Java** services with `createForCurrentTeam()`  
3. **Python** agents writing ops state  
4. A **Node/React** peer with `createFromEnv()`  

What we still did not have was a first-class **Angular** peer that spoke the same language — env identity, in-memory tree, live deltas — without lying about where Connect tokens live.

I have watched teams paste `KIPONOS_ID` into `environment.ts` because “the SPA needs live config.” That is how you turn a visitor’s browser into a credential vault with a change-detection cycle. Redeploying Angular to flip `demo/status-wall/status-alpha` is not real-time ops. It is ceremony with a zone.js tax.

Someone in the room said it cleanly:

**“If Angular can inject a hub service the way Spring injects a bean, I’ll stop shipping SPA builds for ops strings.”**

That sentence is the product brief for `@kiponos/angular`.

<!-- medium-img: diagram-bff-tokens.png -->

---

## What went wrong (the human version)

An “Angular app” is usually two things mashed into one word:

| Piece | Runs where | Holds Connect tokens? |
|-------|------------|------------------------|
| Angular SPA | Visitor’s browser | **Never** |
| Node (or any) BFF | Your machine / cluster | **Yes — like Java** |

The engineers were not stupid. Templates needed live status. The mistake was treating the **browser** as the hub participant.

Java never made that mistake: `createForCurrentTeam()` reads process env. React SDK shipped the same honesty. Angular deserved parity — not a special-case SPA token path.

---

## The Super Pattern (DI + Signals, process identity)

```ts
import { Kiponos } from '@kiponos/angular/server';

// Node BFF / SSR — never the browser bundle
const client = Kiponos.createFromEnv(); // or createForCurrentTeam()
await client.connect();
await client.path('demo', 'status-wall').set('status-alpha', 'focus');
```

In Angular (standalone):

```ts
// app.config.ts
import { provideKiponos } from '@kiponos/angular';
export const appConfig = {
  providers: [provideKiponos({ client })], // inject server-created client
};

// component
import { injectKiponos } from '@kiponos/angular';

export class StatusBadge {
  private readonly kip = injectKiponos();
  readonly status = this.kip.value('demo/status-wall/status-alpha', {
    defaultValue: 'idle',
  });
}
```

Hub tree:

```text
demo / status-wall /
  status-alpha
  status-beta
  note
```

`get` is local after bootstrap. Dashboard edits arrive as deltas. Java peers on the same profile see the same keys — no redeploy for any of them.

<!-- medium-img: diagram-hub-peers.png -->

---

## Where the package lives

Public monorepo (ship with this story):

- Angular SDK: `https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-angular-sdk`  
- React SDK: `https://github.com/kiponos-io/kiponos-io/tree/master/sdks/kiponos-react-sdk`  
- Companion Java peer: `examples/java/angular-sdk-hub-peer`

npm package name: **`@kiponos/angular`** — Node ≥18, Angular ≥16 peer, same wire protocol as Java/Python/React (STOMP, header auth, heartbeats).

---

## The example pattern (Java peer watches Angular/Node writes)

`examples/java/angular-sdk-hub-peer` is the cross-language proof:

1. Node process runs `Kiponos.createFromEnv()` (Angular BFF or plain server).  
2. It sets `demo/status-wall/status-alpha`.  
3. Java `createForCurrentTeam()` reads the leaf locally; `afterValueUpdated` fires when the wall flips.  
4. Dashboard shows the same string without refresh.

```java
Kiponos kip = Kiponos.createForCurrentTeam();
String status = kip.path("demo", "status-wall").get("status-alpha", "idle");
kip.afterValueUpdated(e -> {
    // same contract as Angular KiponosService.afterValueUpdated
});
```

How to try: clone `kiponos-io`, open `sdks/kiponos-angular-sdk` and `examples/java/angular-sdk-hub-peer`. Export `KIPONOS_ID` / `KIPONOS_ACCESS` / `KIPONOS` the way the Java SDK always has.

---

## What we refused to ship

| Temptation | Why we said no |
|------------|----------------|
| Tokens in Angular `environment.ts` | Browser is not a team member |
| Query-param auth as the primary story | Fine for experiments; Node headers match Java |
| NgRx-only “config store” without a hub peer | Local store is not multi-process truth |
| Offline LKG as v1 | Java has OfflineMode; Angular v0.1 is ReadyMode honesty |

---

## The moral

People should not have to ship a release to make a decision.

Angular is now a **first-class hub peer** — Signals for the template, DI for the app, process identity for Connect tokens. The fifth voice on the tree does not get a special loophole. It gets the same contract as Java.

If your SPA still embeds secrets to “feel live,” you do not have real-time config. You have a leak with better change detection.

Ship the BFF. Inject the client. Let the hub speak.
