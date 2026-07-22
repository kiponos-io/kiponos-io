---
main_image: https://litter.catbox.moe/9rgz4p.jpg
title: "Graceful Shutdown Window Live"
published: false
tags: java, devops, architecture, kiponos
description: "drain window live"
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-sre-graceful-shutdown-ms.md
---

**The Aha:** drain window live

You already know the knob. You already know the incident. What hurts is the **ceremony** between decision and effect.

## Config hell, one key

```yaml
# logical tree
examples/sre-graceful-shutdown-ms/drain-ms: 15000
```

## What the process does

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("drain-ms=" + read(p, "drain-ms", "15000"));
            System.out.println("drain window live");
            Thread.sleep(1500L);
        } finally {
            k.disconnect();
        }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("examples").folderOrCreate("sre-graceful-shutdown-ms");
        if (!f.hasKey("drain-ms")) {
            f.set("drain-ms", "15000");
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
cd kiponos-io/examples/java/sre-graceful-shutdown-ms
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Full golden example: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/sre-graceful-shutdown-ms](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/sre-graceful-shutdown-ms)

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
