# The Strategy Pattern Promised Runtime Freedom — Then We Redeployed to Change It

*A traveler’s note from a checkout war room: the algorithm that only swapped after CI.*

---

I have sat through more design reviews where someone wrote **“runtime strategy”** on a whiteboard than I care to count.

The boxes looked honest. Interface on top. Three concrete classes. A context that “selects.” Someone always says the comforting sentence:

**“So we can swap the algorithm without touching callers.”**

What they mean — and what the jar actually does — are different animals.

In the rooms I keep landing in, “runtime” quietly means *the next green build*. Merchandising wants loyalty for two hours of a flash sale. Risk wants flat pricing while a partner is sick. Ops wants a flip **before** the conversion graph finishes falling. Someone opens a PR to change a Spring bean name. CI runs. A jar rolls. Half the replicas still price the old way. The sale ends. The postmortem blames process.

That is not Strategy. That is a ceremony with a shopping cart attached.

---

## What went wrong (the human version)

The engineers were not stupid. The GoF pattern was not wrong.

The problem was the **path to choose**.

In the frozen form, selection lives next to code:

- a hard-coded default  
- a bean name in YAML that only ships with the artifact  
- a `switch` that nobody wants to touch at 14:12 on a Friday  

So every real business change becomes a release discussion. Growth wants **loyalty**. Margin wants **volume**. Fraud wants **flat**. The jar becomes a hostage.

I once heard a lead say, half joking, half broken:

**“If we could just flip which mind prices the cart without a deploy, I’d sleep.”**

That sentence is the whole product brief.

<!-- medium-img: diagram-strategy-gof-vs-super.png -->

---

## The Super Pattern (live selection, not a redeploy)

We keep the classic shape — interface, implementations, context.

We move **which algorithm is thinking** (and the knobs those algorithms need) into a live hub:

```text
patterns / strategy / checkout / active          = flat | volume | loyalty
patterns / strategy / checkout / volume-threshold = 10000
patterns / strategy / checkout / loyalty-bps      = 150
```

Every `priceCart()` does a **local** hub read, then runs pure Java. No HTTP on the hot path. No redeploy to change who prices the cart.

That is the Super Pattern:

> Gang of Four structure + live policy tree = behavior that can change while money is still moving.

<!-- medium-img: diagram-strategy-hub-flow.png -->

---

## The example (standalone Java)

Published under:

**`examples/java/pattern-strategy-live-router`** on [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-strategy-live-router)

It connects with `Kiponos.createForCurrentTeam()`, ensures the policy folder, prices a demo cart, and prints which strategy ran:

```java
static Quote priceCart(Folder policy, long cartCents, boolean loyaltyMember) {
    String id = readActive(policy); // local get() of "active"
    PricingStrategy strategy = STRATEGIES.getOrDefault(id, STRATEGIES.get("flat"));
    StrategyContext ctx = new StrategyContext(
            cartCents,
            loyaltyMember,
            readInt(policy, "volume-threshold", 10_000),
            readInt(policy, "loyalty-bps", 150)
    );
    long total = strategy.priceCents(ctx);
    return new Quote(id, cartCents, total, strategy.describe(ctx));
}

static Folder ensureStrategyFolder(Kiponos kiponos) {
    Folder checkout = kiponos.getRootFolder()
        .folderOrCreate("patterns")
        .folderOrCreate("strategy")
        .folderOrCreate("checkout");
    if (!checkout.hasKey("active")) {
        checkout.set("active", "flat");
    }
    // volume-threshold, loyalty-bps seeded the same way
    return checkout;
}
```

Ops changes `active` on the hub. You re-run (or keep the process listening). **No jar rebuild** to swap the mind that prices the next cart.

---

## How to try it

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-strategy-live-router
cp kiponos.local.env.example kiponos.local.env   # tokens from kiponos.io → Connect
./gradlew test run
```

Then flip `patterns/strategy/checkout/active` on the Kiponos dashboard (or another SDK client) and run again. The printed strategy should follow the hub — not the last release.

Full source + tests:  
https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-strategy-live-router

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

Misplace a row and you either freeze the business or invite unreviewed code into production via a text box.

---

## What this is not

- Not “delete Strategy and put if-else in the dashboard.”  
- Not secrets or card data in the hub.  
- Not free-form code injection — allowlisted strategy ids only.

The algorithms stay reviewed code. The **choice among them** becomes operational.

---

## War-room protocol (keep this boring)

1. Name the hub path: `patterns/strategy/checkout/*`  
2. Speak the clamp before anyone types (max bps, allowlisted ids only)  
3. Write the reason code with the change (`peak`, `margin`, `drill`)  
4. Watch conversion and margin for five minutes  
5. Revert or step — never leave a “temporary” strategy as the silent default  
6. Postmortem line: who moved `active`, from→to, whether automation should own the next flip  

Boring checklists survive 3pm flash sales. Clever ones do not.

---

## The moral

**People should not have to ship a release to make a decision.**

Strategy always promised freedom of behavior. Freedom that only moves at CI speed is not freedom — it is a delayed vote.

Ship the selection path once. Leave the jar alone when the only thing that changed is **which algorithm should think next**.

---

*Example + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-strategy-live-router](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-strategy-live-router)*
