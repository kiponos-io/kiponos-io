---
title: "Flyweight Super Pattern — Cache Cap Live"
published: false
tags: java, devops, architecture, kiponos
description: "flyweight cache size live"
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-flyweight-live-cache.md
---

**The Aha:** flyweight cache size live

You already know the knob. You already know the incident. What hurts is the **ceremony** between decision and effect.

## Config hell, one key

```yaml
# logical tree
patterns/flyweight/glyphs/max-entries: 3
```

## What the process does

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            Map<String, String> cache = new LinkedHashMap<>();
            int max = readInt(p, "max-entries", 3);
            for (String g : List.of("A", "B", "C", "D")) {
                put(cache, max, g, "glyph-" + g);
            }
            System.out.println("cache=" + cache.keySet() + " max=" + max);
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("flyweight").folderOrCreate("glyphs");
        if (!f.hasKey("max-entries")) f.set("max-entries", "3");
        return f;
    }
    static void put(Map<String, String> cache, int max, String k, String v) {
        cache.put(k, v);
        while (cache.size() > max) {
            String first = cache.keySet().iterator().next();
            cache.remove(first);
        }
```

Hot path: **local memory** after connect. Change lands as a WebSocket delta from [Kiponos.io](https://kiponos.io).

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-flyweight-live-cache
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Full golden example: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-flyweight-live-cache](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-flyweight-live-cache)

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
