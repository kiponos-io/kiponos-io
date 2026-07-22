---
title: "Feature Kill Switch Live"
published: false
tags: java, devops, architecture, kiponos
description: "feature kill switch live"
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-sre-feature-kill-switch.md
---

**The Aha:** feature kill switch live

You already know the knob. You already know the incident. What hurts is the **ceremony** between decision and effect.

## Config hell, one key

```yaml
# logical tree
examples/sre-feature-kill-switch/enabled: yes
```

## What the process does

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("enabled=" + read(p, "enabled", "yes"));
            System.out.println("feature kill switch live");
            Thread.sleep(1500L);
        } finally {
            k.disconnect();
        }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("examples").folderOrCreate("sre-feature-kill-switch");
        if (!f.hasKey("enabled")) {
            f.set("enabled", "yes");
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
cd kiponos-io/examples/java/sre-feature-kill-switch
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Full golden example: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/sre-feature-kill-switch](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/sre-feature-kill-switch)

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
