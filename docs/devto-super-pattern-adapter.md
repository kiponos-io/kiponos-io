---
title: "Adapter Was Always the Wrong Layer to Redeploy (Kiponos Super Patterns)"
published: true
tags: java, designpatterns, payments, devops
description: Swap stripe|adyen|braintree from the hub. GoF Adapter becomes a Super Pattern when provider selection is live config.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-super-pattern-adapter.md
main_image: https://files.catbox.moe/qrzq9b.jpg

## Operational checklist

Before you electrify a pattern in production:

1. **Name the hub path** so humans can find it under pressure (`patterns/...`).  
2. **Default safely** — cold start without the hub still works (fail closed where money is involved).  
3. **Allowlist writers** — who can `set()` this tree (dashboard roles, automation identities).  
4. **Log the effective value** on decision points (not every get — the decision).  
5. **Rehearse the flip** in staging with the same example module you ship in the article.  
6. **Document the kill path** — how to revert hub values in one sentence.

If you skip the checklist, you did not build a Super Pattern. You built a remote foot-gun.

## Related reading

- [Rewriting the Gang of Four](https://dev.to/kiponos/rewriting-the-gang-of-four-true-real-time-config-turns-design-patterns-into-super-patterns-nii)  
- [Strategy selection live](https://dev.to/kiponos/the-strategy-pattern-still-required-a-deploy-until-we-made-selection-live-kiponos-super-patterns-1dgm)  
- Getting started: [GitHub GETTING-STARTED](https://github.com/kiponos-io/kiponos-io/blob/master/GETTING-STARTED.md)


---

**The Aha:** Adapters translate APIs. Super adapters let ops choose **which** translation is live without a jar. Payment outages do not wait for your release train.

When Stripe limps, you want Adyen on the next cart — not after CI turns green.

I have heard the war-room version of this more times than I can count:

**“Failover the PSP. We will ship a config change.”**

They meant: open a PR, wait for pipeline, roll pods, hope canaries like money paths.

The adapters already existed. The **selection** was still a deploy.

## The problem: translation is flexible; choice is not

Gang of Four Adapter makes one interface work with another. Excellent.

What freezes:

| Belief | Production |
|--------|------------|
| “We have adapters for every PSP” | Default is a constant or Spring profile |
| “Failover is architecture” | Failover is a release |
| “Currency is static” | Currency and provider both need mid-incident moves |
| “Feature flags fix this” | Second control plane, still slow |

Adapters hide vendor shape. They do **not**, by themselves, hide the redeploy when the vendor **choice** must move.

Checkout is a trust ceremony. The customer already decided. Your adapter stack is not allowed to wait for GitHub Actions.

## The Aha: Adapter + live provider id = Super Pattern

Keep every adapter class. Put **which port is active** in [Kiponos.io](https://kiponos.io):

```yaml
patterns/
  adapter/
    checkout/
      provider: stripe      # stripe | adyen | braintree
      currency: USD
```

Hot path:

```java
Folder policy = kiponos.path("patterns", "adapter", "checkout");
String provider = policy.getString("provider");   // local memory
String currency = policy.getString("currency");

PaymentPort port = switch (provider) {
    case "adyen" -> new AdyenAdapter();
    case "braintree" -> new BraintreeAdapter();
    default -> new StripeAdapter();
};
return port.charge(cents, currency);
```

Ops sets `provider=adyen`. The next charge uses Adyen. Same jar. Same cart code. Different translation.

Remote SDKs can write the same tree when automation detects elevated Stripe error rates — humans and bots share one hub.

## What stays versioned vs live

| Versioned (jar) | Live (hub) |
|-----------------|------------|
| Stripe/Adyen/Braintree adapter code | `provider` |
| Idempotency / retry wrappers | `currency` |
| Metrics names | optional timeout knobs per provider |
| PCI boundary design | never secrets — keep keys in vault |

**Never** put API secrets in the public hub tree. Super Patterns select **behavior**, not credentials.

## Architecture

```text
CheckoutService
      │
      ▼
  resolve provider  ◀── Kiponos local get("provider")
      │
      ├── StripeAdapter
      ├── AdyenAdapter
      └── BraintreeAdapter
             │
             ▼
           PSP HTTPS
```

Adapters remain code. **Selection** is hub state. Hot path never polls the hub; the Java SDK applies WebSocket deltas to an in-process cache.

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-adapter-live-psp
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Runnable: [pattern-adapter-live-psp](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-adapter-live-psp)

Try it tonight:

1. Run with default `stripe` — prove the path.  
2. Flip hub `provider` to `adyen` — next run selects Adyen without rebuild.  
3. Change `currency` — prove both knobs are live.  
4. Time how long a real PSP failover would take in your pipeline vs a hub write.

## Scenarios

| Moment | Frozen default | Super Pattern |
|--------|----------------|---------------|
| Primary PSP timeout storm | Hotfix | `provider=secondary` |
| Currency experiment | Deploy | `currency` live |
| Regional regulation | Branch | Same jar, hub profile per region |
| Cost optimization | Ticket for next sprint | Prefer cheaper rail live |

## When not to swap live

- Brand-new PSP with incomplete adapter tests  
- Schema/protocol changes that need a coordinated rollout  
- Legal freezes that require signed release notes  

Super Adapter is for **operational provider selection**, not for inventing untested integrations under fire.

## Moral

Adapters hide vendors. Super Patterns hide **redeploys** when the vendor choice must move.

If failover still means “open a PR,” you do not have multi-PSP architecture — you have multi-PSP **source code**.

---

*Series: Kiponos Super Patterns. Intro: [Rewriting the Gang of Four](https://dev.to/kiponos/rewriting-the-gang-of-four-true-real-time-config-turns-design-patterns-into-super-patterns-nii).*

*Runnable: [pattern-adapter-live-psp](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-adapter-live-psp) · [kiponos.io](https://kiponos.io)*

## SDK note

Kiponos ships **Java** (Spring Boot 2 and 3 patterns) and **Python** SDKs. This essay uses the Java example; the same hub tree is readable from Python workers with the same local-get / WebSocket-delta model. Do not invent a third language SDK for this pattern — if you are not on Java or Python, keep selection in your existing control plane and treat this as the design target.

## Why this is not “just another flag”

Feature flags are often product gates. Super Patterns are **ops posture on a Gang of Four shape**.

You still allowlist keys. You still test defaults. You still refuse secrets in the hub. What changes is the **distance between human judgment and the next request** — from a release train to a hub write.

That is the entire point of electrifying the patterns.
