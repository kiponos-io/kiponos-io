---
title: "Adapter Was Always the Wrong Layer to Redeploy (Kiponos Super Patterns)"
published: false
tags: java, designpatterns, payments, devops
description: Swap stripe|adyen|braintree from the hub. GoF Adapter becomes a Super Pattern when provider selection is live config.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-super-pattern-adapter.md
---

**The Aha:** Adapters translate APIs. Super adapters let ops choose *which* translation is live without a jar.

Payment outages do not wait for your release train. When Stripe limps, you want Adyen on the next cart — not after CI.

## Hub tree

```yaml
patterns:
  adapter:
    checkout:
      provider: stripe   # stripe | adyen | braintree
      currency: USD
```

## Snippet

```java
PaymentPort port = switch (read(policy, "provider", "stripe")) {
    case "adyen" -> new AdyenAdapter();
    case "braintree" -> new BraintreeAdapter();
    default -> new StripeAdapter();
};
return port.charge(cents, currency);
```

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-adapter-live-psp
./gradlew test run
```

## Moral

Adapters hide vendors. Super Patterns hide **redeploys** when the vendor choice must move.

---
*Runnable: [pattern-adapter-live-psp](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-adapter-live-psp)*
