---
main_image: https://litter.catbox.moe/ugmnm4.jpg
title: "Iterator Super Pattern — Page Size Live"
published: false
tags: java, devops, architecture, kiponos
description: "pagination page size live"
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-iterator-live-page-size.md
---

**The Aha:** pagination page size live

You already know the knob. You already know the incident. What hurts is the **ceremony** between decision and effect.

## Config hell, one key

```yaml
# logical tree
patterns/iterator/catalog/page-size: 25
```

## What the process does

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            int page = readInt(p, "page-size", 25);
            List<String> items = List.of("a","b","c","d","e","f","g");
            System.out.println("page0=" + pageOf(items, page, 0));
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("iterator").folderOrCreate("catalog");
        if (!f.hasKey("page-size")) f.set("page-size", "25");
        return f;
    }
    static List<String> pageOf(List<String> items, int pageSize, int pageIndex) {
        int from = Math.max(0, pageIndex * pageSize);
        if (from >= items.size()) return List.of();
        int to = Math.min(items.size(), from + pageSize);
        return items.subList(from, to);
    }
    static int readInt(Folder p, String k, int d) {
        try {
            if (!p.hasKey(k)) return d;
```

Hot path: **local memory** after connect. Change lands as a WebSocket delta from [Kiponos.io](https://kiponos.io).

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-iterator-live-page-size
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Full golden example: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-iterator-live-page-size](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-iterator-live-page-size)

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
