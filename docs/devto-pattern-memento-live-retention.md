---
main_image: https://litter.catbox.moe/qg4k6e.jpg
title: "Memento Super Pattern — Snapshot Retention Live"
published: false
tags: java, devops, architecture, kiponos
description: "undo depth live"
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-memento-live-retention.md
---

**The Aha:** undo depth live

You already know the knob. You already know the incident. What hurts is the **ceremony** between decision and effect.

## Config hell, one key

```yaml
# logical tree
patterns/memento/editor/max-snapshots: 3
```

## What the process does

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            Deque<String> snaps = new ArrayDeque<>();
            int max = readInt(p, "max-snapshots", 3);
            for (String s : List.of("v1", "v2", "v3", "v4")) push(snaps, max, s);
            System.out.println("snapshots=" + snaps + " max=" + max);
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("memento").folderOrCreate("editor");
        if (!f.hasKey("max-snapshots")) f.set("max-snapshots", "3");
        return f;
    }
    static void push(Deque<String> snaps, int max, String s) {
        snaps.addLast(s);
        while (snaps.size() > max) snaps.removeFirst();
    }
    static int readInt(Folder p, String k, int d) {
        try {
            if (!p.hasKey(k)) return d;
            return Integer.parseInt(p.get(k).trim());
```

Hot path: **local memory** after connect. Change lands as a WebSocket delta from [Kiponos.io](https://kiponos.io).

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-memento-live-retention
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Full golden example: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-memento-live-retention](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-memento-live-retention)

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
