---
title: "Bridge Super Pattern — Implementor Chosen Live"
published: false
tags: java, devops, architecture, kiponos
description: "smtp|ses|sendgrid selection live"
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-bridge-live-implementor.md
---

**The Aha:** smtp|ses|sendgrid selection live

You already know the knob. You already know the incident. What hurts is the **ceremony** between decision and effect.

## Config hell, one key

```yaml
# logical tree
patterns/bridge/notify/implementor: smtp
```

## What the process does

```java
    public static void main(String[] args) throws InterruptedException {
        String msg = args.length > 0 ? args[0] : "bridge demo";
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder policy = ensure(k);
            String out = new Notification(createImplementor(policy)).send(msg);
            System.out.println("========================================");
            System.out.println("  Super Pattern: Live Bridge");
            System.out.println("  result: " + out);
            System.out.println("========================================");
            Thread.sleep(2000L);
        } finally { k.disconnect(); }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("bridge").folderOrCreate("notify");
        if (!f.hasKey("implementor")) f.set("implementor", "smtp");
        return f;
    }

    static MessageSender createImplementor(Folder policy) {
        String id = read(policy, "implementor", "smtp").toLowerCase(Locale.ROOT);
        return switch (id) {
            case "ses" -> body -> "ses:" + body;
```

Hot path: **local memory** after connect. Change lands as a WebSocket delta from [Kiponos.io](https://kiponos.io).

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-bridge-live-implementor
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Full golden example: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-bridge-live-implementor](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-bridge-live-implementor)

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
