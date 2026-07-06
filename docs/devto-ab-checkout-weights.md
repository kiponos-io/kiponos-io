---
title: "Shift E-Commerce A/B Checkout Weights in Real Time (Kiponos Java SDK)"
published: true
tags: java, ecommerce, abtesting, realtime
description: Rebalance checkout experiment variants during live traffic without redeploying your Java storefront. Kiponos delivers variant weights via WebSocket deltas with zero-latency local reads.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-ab-checkout-weights.md
main_image: https://files.catbox.moe/dlkkmj.jpg
---

A/B tests are supposed to be scientific. Reality: variant B is crushing it at 10 AM, variant C is hurting conversion at 2 PM, and marketing wants to **reweight the split before the weekend rush** — without a deploy window.

[Kiponos.io](https://kiponos.io) lets your Java checkout service read **live experiment weights** from memory on every session assignment.

## Why not hard-code weights?

```java
int bucket = hash(sessionId) % 100;
if (bucket < 50) return Variant.A;
if (bucket < 80) return Variant.B;
return Variant.C;
```

Those cutoffs (50, 80) usually live in config files. Changing 50→60→70 means:

- Rebuild + redeploy during peak sales
- Or per-request DB lookups (latency + load)

## Live weight model

```yaml
experiments/
  checkout_q2/
    variant_a_weight: 40
    variant_b_weight: 45
    variant_c_weight: 15
    enabled: true
```

```java
@Service
public class CheckoutExperiment {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();

    public Variant assign(String sessionId) {
        var exp = kiponos.path("experiments", "checkout_q2");
        if (!exp.getBool("enabled")) return Variant.CONTROL;

        int a = exp.getInt("variant_a_weight");
        int b = exp.getInt("variant_b_weight");
        int c = exp.getInt("variant_c_weight");
        int roll = Math.floorMod(sessionId.hashCode(), a + b + c);

        if (roll < a) return Variant.A;
        if (roll < a + b) return Variant.B;
        return Variant.C;
    }
}
```

Product manager slides weights in Kiponos dashboard → **new sessions** immediately use updated distribution. Existing sessions can stick with assignment cookie — your choice.

## Use cases

| Scenario | Live tweak |
|----------|------------|
| Winner emerging | Shift 70% traffic to best variant |
| Bug in variant C | Set `variant_c_weight: 0` instantly |
| Flash sale | Disable experiment, route 100% to optimized flow |
| Geo test | Separate weight folders per region |

## Zero performance hit

Weight reads are **local cache lookups** — safe inside checkout hot path. Updates arrive as **async WebSocket deltas**.

Try free TeamPro at [kiponos.io](https://kiponos.io). Integration resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for Java. Steer experiments while carts are moving.*