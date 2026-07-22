---
main_image: https://litter.catbox.moe/5ljude.jpg
title: "Prototype Super Pattern — Active Template Live"
published: false
tags: java, devops, architecture, kiponos
description: "which prototype to clone live"
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-prototype-live-registry.md
---

**The Aha:** which prototype to clone live

You already know the knob. You already know the incident. What hurts is the **ceremony** between decision and effect.

## Config hell, one key

```yaml
# logical tree
patterns/prototype/docs/active-template: invoice-v1
```

## What the process does

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println(cloneTemplate(p));
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("prototype").folderOrCreate("docs");
        if (!f.hasKey("active-template")) f.set("active-template", "invoice-v1");
        return f;
    }
    static String cloneTemplate(Folder policy) {
        String id = read(policy, "active-template", "invoice-v1");
        return REGISTRY.getOrDefault(id, REGISTRY.get("invoice-v1")) + " [clone of " + id + "]";
    }
    static String read(Folder p, String k, String d) {
        if (!p.hasKey(k)) return d;
        String r = p.get(k); return r == null || r.isBlank() ? d : r.trim();
    }
    private PrototypeLiveRegistryApp() {}
}
```

Hot path: **local memory** after connect. Change lands as a WebSocket delta from [Kiponos.io](https://kiponos.io).

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-prototype-live-registry
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Full golden example: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-prototype-live-registry](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-prototype-live-registry)

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
