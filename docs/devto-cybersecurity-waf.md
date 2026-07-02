---
title: "Live WAF and Security Rule Tuning — No Java Restart (Kiponos SDK)"
published: true
tags: java, security, devops, realtime
description: Adjust WAF scores, block thresholds, and OWASP rule sensitivity in Java security gateways at runtime. Kiponos WebSocket deltas with zero-latency reads per request.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-cybersecurity-waf.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-waf.jpg
---

During an attack, security teams **tighten WAF thresholds**. During false-positive storms, they **loosen** the same rules. Java API gateways embedded with WAF logic usually encode sensitivity in YAML — change means **rolling restart** while traffic is hostile.

[Kiponos.io](https://kiponos.io) stores rule sensitivity, IP reputation thresholds, and challenge modes in a live tree gateways read on **every request**.

## WAF evaluation path

```java
public WafAction inspect(HttpRequest req) {
    var waf = kiponos.path("waf", "production");
    int score = computeThreatScore(req);
    if (score >= waf.getInt("block_threshold")) {
        return WafAction.block("score");
    }
    if (score >= waf.getInt("challenge_threshold")) {
        return WafAction.challenge();
    }
    if (waf.getBool("geo_block_high_risk") && highRiskGeo(req)) {
        return WafAction.block("geo");
    }
    return WafAction.allow();
}
```

## WAF tree

```yaml
waf/
  production/
    block_threshold: 85
    challenge_threshold: 60
    geo_block_high_risk: true
    sql_injection_sensitivity: 0.8
    rate_limit_rpm: 3000
  modes/
    under_attack: false
    log_only: false
```

## Real-world scenarios

| Scenario | Live tweak |
|----------|------------|
| Credential stuffing | Lower `challenge_threshold` |
| False positives on API | Raise `block_threshold`, enable `log_only` |
| Campaign DDoS | `under_attack: true` preset bundle |
| Zero-day rule trial | Adjust `sql_injection_sensitivity` |

## Performance

Per-request WAF must use **local gets** — same as [rate limits article](https://dev.to/kiponos/change-api-rate-limits-and-circuit-breakers-at-runtime-no-java-redeploy-kiponos-sdk-3d94).

## Getting started

1. [kiponos.io](https://kiponos.io) — `waf/production/*`
2. Externalize thresholds from gateway config
3. Red team drill: tighten rules live; measure block rate change without pod restart

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for Java security. WAF tuning during the attack — not after the deploy.*