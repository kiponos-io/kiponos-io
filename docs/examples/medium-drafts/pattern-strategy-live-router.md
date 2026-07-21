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

## The moral, if you need one on a slide

The Gang of Four already knew systems need freedom of behavior.

What they could not ship in 1994 was a realtime hub where people **and** other machines rewrite the inner variables of a pattern without redeploy.

**Strategy is not a Super Pattern until selection can move at the speed of the business.**

Kiponos.io — the companion that turns Design Patterns into live ones.
