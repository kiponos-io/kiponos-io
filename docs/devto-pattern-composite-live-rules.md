---
main_image: https://litter.catbox.moe/iv9ar1.jpg
title: "Composite Super Pattern — Node Weights Live"
published: false
tags: java, devops, architecture, kiponos
description: "composite scoring weights live"
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-composite-live-rules.md
---

**The Aha:** composite scoring weights live

You already know the knob. You already know the incident. What hurts is the **ceremony** between decision and effect.

## Config hell, one key

```yaml
# logical tree
patterns/composite/score/nodes: base:1,risk:2
```

## What the process does

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("score=" + score(p, Map.of("base", 10, "risk", 3, "loyalty", 5)));
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("composite").folderOrCreate("score");
        if (!f.hasKey("nodes")) f.set("nodes", "base:1,risk:2,loyalty:1");
        if (!f.hasKey("enabled")) f.set("enabled", "base,risk,loyalty");
        return f;
    }
    static double score(Folder policy, Map<String, Integer> values) {
        Set<String> enabled = new HashSet<>();
        for (String e : read(policy, "enabled", "").split(",")) {
            if (!e.trim().isEmpty()) enabled.add(e.trim().toLowerCase());
        }
        double sum = 0, wsum = 0;
        for (String part : read(policy, "nodes", "").split(",")) {
            String[] kv = part.trim().split(":");
            if (kv.length != 2) continue;
            String id = kv[0].trim().toLowerCase();
```

Hot path: **local memory** after connect. Change lands as a WebSocket delta from [Kiponos.io](https://kiponos.io).

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-composite-live-rules
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Full golden example: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-composite-live-rules](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-composite-live-rules)

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
