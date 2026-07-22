# The Fraud Chain Was Correct — Until Someone Reordered Reality Without a Deploy

*A traveler’s note on Chain of Responsibility, 3am velocity attacks, and the Super Pattern that lets ops rewrite the order of judgment live.*

---

I have watched a fraud stack fail not because the handlers were wrong, but because the **order** was wrong for that week of the world.

Amount cap first. Geo second. Velocity third. It looked reasonable in a design review. Then a regional promo made high-value carts normal, and the velocity window started eating good customers while the real bot farm slipped under the amount ceiling.

Someone said the sentence that always costs a night:

**“We’ll cut a PR to reorder the chain.”**

A PR. To change the order of thought.

---

## What Chain of Responsibility was always trying to say

Chain is an admission: **no single rule owns the decision.**

Each handler may pass or stop. Order matters. Parameters matter. Classic GoF gives you the shape. It does not give you a control plane when attackers change tactics at dinner.

---

## Super Pattern: Live Handler Chain

We keep handlers as pure functions. We move **order + knobs** into [Kiponos.io](https://kiponos.io):

```text
patterns / chain / fraud / order              = amount-cap,geo,velocity
patterns / chain / fraud / amount-cap-cents    = int
patterns / chain / fraud / blocked-countries   = csv
patterns / chain / fraud / velocity-max        = int
```

`evaluate()` reads the live CSV order, runs handlers left to right, first reject wins. Local `get()` only — no hub RTT on the payment path.

<!-- medium-img: diagram-chain-handlers.png -->

### Snippet (decision core)

```java
for (String id : order) {
    Decision step = handlers.get(id).check(payment);
    if (!step.allowed()) return step; // stop the chain
}
return Decision.pass("all handlers passed");
```

---

## Clone and run the golden example

**Java (primary):**

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-chain-live-fraud
cp kiponos.local.env.example kiponos.local.env   # tokens from kiponos.io → Connect
./gradlew test run
./gradlew run --args='25000 US 2'
```

**Python parity:** `examples/python/pattern-chain-live-fraud/`

Full source + logic tests + golden E2E live in the repo. This article only shows the nerve.

---

## Old world vs Super Pattern

| Move | Old world | Super Pattern |
|------|-----------|---------------|
| Reorder handlers | PR → CI → deploy | Hub `order` CSV |
| Raise amount cap | Config file + roll | Live `amount-cap-cents` |
| Block a country mid-incident | Hope the flag ship lands | Live `blocked-countries` |

---

## The moral

**A chain of responsibility is only as fast as your ability to change who speaks first.**

Ship the judgment path once. Leave the order of judgment in the hub.

---

*Example + tests: [github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-chain-live-fraud](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-chain-live-fraud)*
