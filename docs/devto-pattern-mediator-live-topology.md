---
title: "Mediator Super Pattern — Who Talks to Whom Live"
published: false
tags: java, devops, architecture, kiponos
description: "topology edges live"
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-mediator-live-topology.md
---

**The Aha:** topology edges live

You already know the knob. You already know the incident. What hurts is the **ceremony** between decision and effect.

## Config hell, one key

```yaml
# logical tree
patterns/mediator/chat/edges: alice>bob
```

## What the process does

```java
    public static void main(String[] args) throws Exception {
        String from = args.length > 0 ? args[0] : "alice";
        String to = args.length > 1 ? args[1] : "bob";
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println(route(p, from, to));
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("mediator").folderOrCreate("chat");
        if (!f.hasKey("edges")) f.set("edges", "alice>bob,bob>carol,alice>carol");
        return f;
    }
    static String route(Folder policy, String from, String to) {
        String edge = from.toLowerCase(Locale.ROOT) + ">" + to.toLowerCase(Locale.ROOT);
        Set<String> edges = new HashSet<>();
        for (String part : read(policy, "edges", "").split(",")) {
            String e = part.trim().toLowerCase(Locale.ROOT).replace(" ", "");
            if (!e.isEmpty()) edges.add(e);
        }
        return edges.contains(edge) ? "deliver " + edge : "blocked " + edge;
    }
```

Hot path: **local memory** after connect. Change lands as a WebSocket delta from [Kiponos.io](https://kiponos.io).

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-mediator-live-topology
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Full golden example: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-mediator-live-topology](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-mediator-live-topology)

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
