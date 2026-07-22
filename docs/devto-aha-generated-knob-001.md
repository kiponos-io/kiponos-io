---
main_image: https://litter.catbox.moe/flc3pc.jpg
title: "Live Ops Knob 1: Tune Without Redeploy"
published: false
tags: java, devops, architecture, kiponos
description: "Generic live knob knob-1 for continuous stream demo 1"
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-generated-knob-001.md
---

**The Aha:** Generic live knob knob-1 for continuous stream demo 1

You already know the knob. You already know the incident. What hurts is the **ceremony** between decision and effect.

## Config hell, one key

```yaml
# logical tree
examples/aha-generated-knob-001/knob-1: 11
```

## What the process does

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("knob-1=" + read(p, "knob-1", "11"));
            System.out.println("Generic live knob knob-1 for continuous stream demo 1");
            Thread.sleep(1500L);
        } finally {
            k.disconnect();
        }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("examples").folderOrCreate("aha-generated-knob-001");
        if (!f.hasKey("knob-1")) {
            f.set("knob-1", "11");
        }
        return f;
    }

    static String read(Folder p, String key, String def) {
        if (!p.hasKey(key)) {
            return def;
        }
```

Hot path: **local memory** after connect. Change lands as a WebSocket delta from [Kiponos.io](https://kiponos.io).

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/aha-generated-knob-001
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Full golden example: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-generated-knob-001](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-generated-knob-001)

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
