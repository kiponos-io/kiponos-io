---
title: "Singleton Super Pattern — One Instance Live Policy"
published: false
tags: java, devops, architecture, kiponos
description: "singleton policy knobs live"
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-singleton-live-policy.md
---

**The Aha:** singleton policy knobs live

You already know the knob. You already know the incident. What hurts is the **ceremony** between decision and effect.

## Config hell, one key

```yaml
# logical tree
patterns/singleton/gate/mode: open
```

## What the process does

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("mode=" + get().mode(p));
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("singleton").folderOrCreate("gate");
        if (!f.hasKey("mode")) f.set("mode", "open");
        return f;
    }
    String mode(Folder policy) {
        if (!policy.hasKey("mode")) return "open";
        String r = policy.get("mode");
        return r == null || r.isBlank() ? "open" : r.trim().toLowerCase();
    }
}
```

Hot path: **local memory** after connect. Change lands as a WebSocket delta from [Kiponos.io](https://kiponos.io).

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-singleton-live-policy
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Full golden example: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-singleton-live-policy](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-singleton-live-policy)

## When this is enough

| Use live hub | Prefer release |
|--------------|----------------|
| Timeouts, caps, enable flags | Schema / protocol changes |
| Incident posture | Security-sensitive crypto |
| Regional tuning | License / legal text freezes |

## Moral

Ship judgment. Leave the jar alone.

---
*Runnable example on GitHub — this post is not a substitute for the tests.*
