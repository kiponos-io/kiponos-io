# The Strategy Pattern Promised Runtime Freedom — Then We Redeployed to Change It

*A traveler’s note on Gang of Four, checkout pricing, and the Super Pattern that finally keeps the promise.*

---

I have drawn the Strategy pattern on more whiteboards than I care to count.

Boxes. Arrows. A tidy interface. Three concrete classes. Someone always nods and says: **“So we can swap algorithms at runtime.”**

Runtime.

That word does a lot of work in a design-review room. It sounds like freedom. It sounds like the system will listen when the business changes its mind at 2:14pm on a Friday.

Then production arrives, and “runtime” quietly means *the next release train*.

---

## What Strategy was always trying to say

Strategy is not about clever class names. At its core it is an admission:

**Behavior is not permanent.**

Pricing formulas, routing policies, fraud scores, recommendation weights — they want to move when the world moves. The GoF pattern gives you a clean place to put those algorithms. It does *not* give you a nervous system to choose among them while the process is already hot.

So teams do what teams do:

- hard-code a default  
- wire a Spring bean name  
- bury a `switch` in a service  
- wait for CI  

The pattern’s *shape* is flexible. The *selection* is still a fossil.

---

## Super Pattern: Live Strategy Router

We keep the classic shape — interface, implementations, context.

We move the **selection** (and the knobs the strategies need) into [Kiponos.io](https://kiponos.io):

```text
patterns / strategy / checkout / active
patterns / strategy / checkout / volume-threshold
patterns / strategy / checkout / loyalty-bps
```

- `active` = `flat` | `volume` | `loyalty`  
- volume strategy reads its threshold live  
- loyalty strategy reads discount bps live  

Every `priceCart()` does a **local** hub read, then runs pure Java. No HTTP on the hot path. No redeploy to change who prices the cart.

That is the Super Pattern:

**Gang of Four structure + live policy tree = behavior that can change while money is still moving.**

<!-- medium-img: diagram-strategy-gof-vs-super.png -->

---

## Why Kiponos is the companion Strategy always needed

Not only humans flip `active` in the dashboard.

Another service — promotions, risk, a partner SDK — can **programmatically** `set` the same keys. WebSocket deltas arrive. The in-process tree updates. The next checkout decision already belongs to a different algorithm.

Strategy always wanted remote authority over *which mind is thinking*. Kiponos is that authority without a restart.

---

## The example (standalone Java)

Runnable under:

```bash
examples/java/pattern-strategy-live-router
./gradlew test run
```

It ensures the policy folder, prints the active strategy, and prices a demo cart. Change `active` in the hub. Run again. The jar is innocent. The selection is live.

```text
patterns / strategy / checkout / active = volume
```

<!-- medium-img: diagram-strategy-hub-flow.png -->

---

## What this is not

- Not “delete the Strategy pattern and put if-else in the dashboard.”  
- Not secrets or card data in the hub.  
- Not free-form code injection — allowlisted strategy ids only.

The algorithms stay reviewed code. The **choice among them** becomes operational.

---

---

## Black Friday, not Black Pipeline

Picture merchandising at 14:12. Loyalty is winning on AOV. Volume is losing on margin. Someone says: “Flip to loyalty for two hours.”

In the fossil world that is a ticket, a PR, a review, a green build, a cautious roll. By the time the jar knows, the sale is over or the margin is gone.

In the Super Pattern world it is a hub write:

```text
patterns / strategy / checkout / active = loyalty
patterns / strategy / checkout / loyalty-bps = 150
```

The next cart prices under loyalty. The jar never left the node. When the window ends, flip back. The postmortem has from→to, not “we missed the train.”

---

## What stays versioned vs live

| Versioned in the jar | Live in the hub |
|----------------------|-----------------|
| Strategy interface & implementations | `active` strategy id |
| Allowlist of strategy names | Volume threshold |
| Clamp logic (min/max bps) | Loyalty basis points |
| Pure pricing math | Which mind is thinking |

That table is the entire architecture argument. Misplace a row and you either freeze the business or invite unreviewed code into production via a text box.

---

## War-room protocol (paste into the runbook)

1. Name the hub path: `patterns/strategy/checkout/*`  
2. Speak the clamp before anyone types (max bps, allowlisted ids only)  
3. Write the reason code with the change (`peak`, `margin`, `drill`)  
4. Watch conversion and margin for five minutes  
5. Revert or step — never leave a “temporary” strategy as the silent default  
6. Postmortem line: who moved `active`, from→to, whether automation should own the next flip  

---

## Testing that does not lie

- Unit-test each strategy with fixed numbers (no network).  
- Unit-test the router with a fake policy map: unknown `active` → safe default.  
- Integration-test the hub path against the public sandbox when you can.  
- Never assert wall-clock WebSocket delivery in CI.

The example module under `examples/java/pattern-strategy-live-router` is the golden path — clone it before inventing a second router.

---

## The moral, if you need one on a slide

The Gang of Four already knew systems need freedom of behavior.

What they could not ship in 1994 was a realtime hub where people **and** other machines rewrite the inner variables of a pattern without redeploy.

**Strategy is not a Super Pattern until selection can move at the speed of the business.**

Kiponos.io — the companion that turns Design Patterns into live ones.
