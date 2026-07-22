---
main_image: https://litter.catbox.moe/i0to6u.jpg
title: "Builder Super Pattern — Defaults and Severity Live"
published: false
tags: java, devops, architecture, kiponos
description: "builder defaults from hub"
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-builder-live-defaults.md
---

**The Aha:** builder defaults from hub

You already know the knob. You already know the incident. What hurts is the **ceremony** between decision and effect.

## Config hell, one key

```yaml
# logical tree
patterns/builder/report/page-size: 50
```

## What the process does

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            Report r = build(p, args.length > 0 ? args[0] : "Q1");
            System.out.println(r);
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("builder").folderOrCreate("report");
        if (!f.hasKey("page-size")) f.set("page-size", "50");
        if (!f.hasKey("include-charts")) f.set("include-charts", "yes");
        if (!f.hasKey("severity")) f.set("severity", "warn");
        return f;
    }
    static Report build(Folder policy, String title) {
        int page = readInt(policy, "page-size", 50);
        boolean charts = truthy(read(policy, "include-charts", "yes"));
        String severity = read(policy, "severity", "warn");
        if (page < 1) {
            if ("error".equalsIgnoreCase(severity)) throw new IllegalArgumentException("page-size");
            page = 50;
        }
```

Hot path: **local memory** after connect. Change lands as a WebSocket delta from [Kiponos.io](https://kiponos.io).

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-builder-live-defaults
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Full golden example: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-builder-live-defaults](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-builder-live-defaults)

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
