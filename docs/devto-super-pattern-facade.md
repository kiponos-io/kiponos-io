---
title: "Stable Facade, Live Guts (Kiponos Super Patterns)"
published: true
tags: java, designpatterns, architecture, devops
description: checkout() stays stable while tax bps, inventory check, and notify knobs move in Kiponos — GoF Facade as a Super Pattern.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-super-pattern-facade.md
main_image: https://files.catbox.moe/7c0j6h.jpg
---

**The Aha:** Facades promise a simple API. Super facades keep that promise while the **guts knobs** move — without redeploying the jar callers already trust.

There is a particular comfort in a clean checkout method.

```java
order.checkout(cart);
```

No tax tables in the controller. No inventory handshake in the handler. No notification fan-out littering the happy path. The facade did its job: **one verb, many systems**.

Then Black Friday happens.

Tax rules change mid-day. Inventory check must soft-fail when the stock service limps. Marketing wants SMS off because the vendor is on fire. And someone says the sentence that ruins the design:

**“We need to change what checkout does — but we have to redeploy.”**

The facade stayed simple. The **judgment behind it** was still a release train.

## The problem: a stable door with frozen furniture behind it

Gang of Four Facade is honest about its purpose: provide a unified interface to a set of interfaces in a subsystem.

What most codebases actually ship:

| Belief | Production |
|--------|------------|
| “Facade hides complexity” | Complexity is hidden **and frozen** in constructors |
| “Callers stay clean” | Ops cannot retune guts without a PR |
| “Defaults are fine” | Defaults become incident root causes |
| “Feature flags cover this” | Another system, another deploy path |

The callers were never the problem. The problem is that **tax bps, inventory posture, and notify on/off** are operational decisions wearing the costume of compile-time wiring.

I have sat with commerce teams who could redraw the subsystem diagram from memory — payment, tax, inventory, notify — and still could not flip “skip inventory check for the next twenty minutes” without a jar.

That is not a facade. That is a **ceremonial door**.

## The Aha: Facade + live guts knobs = Super Pattern

Keep the GoF shape. Callers still see `checkout()`.

Move the **subsystem knobs** into a realtime tree on [Kiponos.io](https://kiponos.io):

```yaml
patterns/
  facade/
    checkout/
      tax-bps: 800              # basis points
      inventory-check: yes      # yes | no
      notify: yes               # yes | no
      soft-fail-inventory: no
```

Hot path (local memory after connect — no per-request hub RTT):

```java
Folder guts = kiponos.path("patterns", "facade", "checkout");
int taxBps = guts.getInt("tax-bps");          // local get
boolean checkInv = guts.getBoolean("inventory-check");
boolean notify = guts.getBoolean("notify");

Money tax = taxService.compute(cart, taxBps);
if (checkInv) {
    inventory.reserve(cart);                  // or soft-fail if knobs say so
}
payment.capture(cart, tax);
if (notify) {
    notifier.orderPlaced(cart);
}
```

Ops raises `tax-bps` for a region. Turns `inventory-check` off when the stock service is in the weeds. Kills `notify` when the SMS vendor is rate-limiting the world.

**Callers never change.** The facade contract stays. The guts breathe.

## What stays code vs what becomes hub state

| Stays in the jar | Lives in the hub |
|------------------|------------------|
| Payment capture protocol | Tax basis points |
| Inventory client | Whether to call inventory tonight |
| Notifier implementations | Whether to notify |
| Order ID generation | Soft-fail posture |

That split is the Super Pattern discipline: **structure is versioned; posture is live**.

## Architecture (mental model)

```text
Controller ──checkout()──▶ CheckoutFacade
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
           TaxService    Inventory       Notifier
              ▲               ▲               ▲
              └──── knobs from Kiponos tree ──┘
                    (local cache + WS deltas)
```

The facade is still the single entry. Kiponos is not another service call on the hot path — the Java SDK holds the latest values **in process**, patched over WebSocket when humans (or remote automation) write the hub.

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-facade-live-knobs
cp kiponos.local.env.example kiponos.local.env   # sandbox credentials
./gradlew test run
```

Runnable tree: [pattern-facade-live-knobs](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-facade-live-knobs)

Try it tonight:

1. Run tests — prove defaults produce a coherent checkout path.  
2. Flip `inventory-check` to `no` in the hub — next run skips reserve without a rebuild.  
3. Change `tax-bps` — tax line moves; method signature does not.  
4. Ask: would you still open a PR to mute notifications mid-incident?

## Scenarios where Super Facade earns its keep

| Moment | Frozen facade guts | Super Pattern |
|--------|--------------------|---------------|
| Tax rate emergency | Redeploy | `tax-bps` live |
| Inventory service outage | Fail closed hard or ship hotfix | `inventory-check=no` or soft-fail |
| Notify vendor meltdown | Comment out code, ship | `notify=no` |
| Regional experiment | Branch per region | Same jar, different hub values |

## When not to put it in the hub

Live knobs are for **posture**, not for inventing a new protocol mid-flight.

- Do **not** put cryptographic algorithms, schema versions, or legal copy that must freeze for audit solely in a dashboard without process.  
- Do put timeouts, enable flags, basis points, and “skip this subsystem tonight” decisions where humans already make them under pressure.

Honest boundaries matter. Super Patterns are not “config everything.”

## Moral

Callers stay calm. Operators stay armed.

If your facade requires a redeploy to change what the subsystems do, you did not hide complexity — you **hid the control panel**.

Ship judgment in the hub. Leave the door painted the same color.

---

*Series: Kiponos Super Patterns — Gang of Four shapes with live selection and knobs. Intro: [Rewriting the Gang of Four](https://dev.to/kiponos/rewriting-the-gang-of-four-true-real-time-config-turns-design-patterns-into-super-patterns-nii).*

*Runnable: [pattern-facade-live-knobs](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-facade-live-knobs) · Product: [kiponos.io](https://kiponos.io)*

## SDK note

Kiponos ships **Java** (Spring Boot 2 and 3 patterns) and **Python** SDKs. This essay uses the Java example; the same hub tree is readable from Python workers with the same local-get / WebSocket-delta model. Do not invent a third language SDK for this pattern — if you are not on Java or Python, keep selection in your existing control plane and treat this as the design target.

