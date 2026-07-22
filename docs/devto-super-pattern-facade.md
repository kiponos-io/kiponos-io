---
main_image: https://litter.catbox.moe/lxoidb.jpg
title: "Stable Facade, Live Guts (Kiponos Super Patterns)"
published: false
tags: java, designpatterns, architecture, devops
description: checkout() stays stable; tax bps, inventory check, and notify knobs live in Kiponos.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-super-pattern-facade.md
---

**The Aha:** Facades promise a simple API. Super facades keep that promise while the **guts knobs** move.

## Hub tree

```yaml
patterns:
  facade:
    checkout:
      tax-bps: 800
      inventory-check: yes
      notify: yes
```

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-facade-live-knobs
./gradlew test run
```

## Moral

Callers stay calm. Operators stay armed.

---
*Runnable: [pattern-facade-live-knobs](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-facade-live-knobs)*
