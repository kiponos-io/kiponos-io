---
title: "Visitor Super Pattern — Visitor Registry Live"
published: false
tags: java, devops, architecture, kiponos
description: "which visitors run live"
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-visitor-live-registry.md
---

**The Aha:** which visitors run live

You already know the knob. You already know the incident. What hurts is the **ceremony** between decision and effect.

## Config hell, one key

```yaml
# logical tree
patterns/visitor/export/visitors: json,csv
```

## What the process does

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println(visit(p, "order-1"));
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("visitor").folderOrCreate("export");
        if (!f.hasKey("visitors")) f.set("visitors", "json,csv");
        return f;
    }
    static List<String> visit(Folder policy, String node) {
        List<String> out = new ArrayList<>();
        for (String v : read(policy, "visitors", "json").split(",")) {
            String id = v.trim().toLowerCase();
            if (id.isEmpty()) continue;
            out.add(id + ".visit(" + node + ")");
        }
        return out;
    }
    static String read(Folder p, String k, String d) {
        if (!p.hasKey(k)) return d;
```

Hot path: **local memory** after connect. Change lands as a WebSocket delta from [Kiponos.io](https://kiponos.io).

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-visitor-live-registry
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Full golden example: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-visitor-live-registry](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-visitor-live-registry)

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
