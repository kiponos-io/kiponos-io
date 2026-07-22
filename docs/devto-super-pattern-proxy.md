---
main_image: https://litter.catbox.moe/1824pj.jpg
title: "Proxy That Can Close the Door Without a Deploy (Kiponos Super Patterns)"
published: false
tags: java, designpatterns, security, devops
description: Live enable/deny + role allow-list for sensitive paths. GoF Proxy as a Super Pattern.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-super-pattern-proxy.md
---

**The Aha:** A proxy that cannot change policy at 2am is just another hardcoded gate.

## Hub tree

```yaml
patterns:
  proxy:
    admin-api:
      enabled: yes
      rate-per-min: 30
      role-allow: operator,admin
```

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-proxy-live-access
./gradlew test run --args=operator
```

## Moral

Close the door from the hub. Leave the jar alone.

---
*Runnable: [pattern-proxy-live-access](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-proxy-live-access)*
