# Super Pattern: Live Strategy Router

**Gang of Four:** Strategy  
**Kiponos Super Pattern:** the *active* algorithm (and its knobs) live in the hub — not in a deploy.

## Problem

Strategy promises “swap algorithms at runtime.” In most codebases, “runtime” still means *next release*: a `switch`, a Spring bean name, or a feature flag that only becomes true after CI.

Checkout is pricing a cart **now**. Merchandising wants volume pricing for the sale hour. Loyalty wants 150 bps off for members. Nobody wants a jar redeploy to pick `flat | volume | loyalty`.

## Super Pattern

```text
patterns / strategy / checkout / active            = flat | volume | loyalty
patterns / strategy / checkout / volume-threshold  = int (cents)
patterns / strategy / checkout / loyalty-bps       = int
```

`priceCart()` reads `active` from the in-memory Kiponos tree (local `get()`), then runs the matching `PricingStrategy`. Ops or a **remote SDK** can `set("active", "volume")` — next decision uses the new algorithm without restart.

## Run

```bash
cd examples/java/pattern-strategy-live-router
cp kiponos.local.env.example kiponos.local.env   # fill tokens
./gradlew test run
# optional: cart cents + loyalty member
./gradlew run --args='12500 yes'
```

## Moral

**Strategy was always about freedom of behavior. Kiponos is the freedom to change that behavior without shipping a release.**
