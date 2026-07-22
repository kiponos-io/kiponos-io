---
title: "Retry Max Attempts Live"
published: false
tags: java, devops, architecture, kiponos
description: "retry budget live"
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-retry-max-attempts.md
---

**The Aha:** retry budget live

You already know the knob. You already know the incident. What hurts is the **ceremony** between decision and effect.

## Config hell, one key

```yaml
# logical tree
examples/aha-retry-max-attempts/max-attempts: 3
```

## What the process does

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("max-attempts=" + read(p, "max-attempts", "3"));
            System.out.println("retry budget live");
            Thread.sleep(1500L);
        } finally {
            k.disconnect();
        }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("examples").folderOrCreate("aha-retry-max-attempts");
        if (!f.hasKey("max-attempts")) {
            f.set("max-attempts", "3");
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
cd kiponos-io/examples/java/aha-retry-max-attempts
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Full golden example: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-retry-max-attempts](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-retry-max-attempts)

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
