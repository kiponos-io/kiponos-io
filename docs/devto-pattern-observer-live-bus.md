---
title: "Observer Super Pattern — Live Subscriber Enable List"
published: false
tags: java, devops, architecture, kiponos
description: "enable/debounce observers live"
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-observer-live-bus.md
---

**The Aha:** enable/debounce observers live

You already know the knob. You already know the incident. What hurts is the **ceremony** between decision and effect.

## Config hell, one key

```yaml
# logical tree
patterns/observer/bus/enabled-subscribers: metrics,audit
```

## What the process does

```java
    public static void main(String[] args) throws Exception {
        String event = args.length > 0 ? args[0] : "order.paid";
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            List<String> trail = publish(p, event);
            System.out.println("event=" + event + " trail=" + trail);
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("observer").folderOrCreate("bus");
        if (!f.hasKey("enabled-subscribers")) f.set("enabled-subscribers", "metrics,audit");
        if (!f.hasKey("debounce-ms")) f.set("debounce-ms", "100");
        return f;
    }
    static List<String> publish(Folder policy, String event) {
        List<String> enabled = csv(read(policy, "enabled-subscribers", "metrics,audit"));
        int debounce = readInt(policy, "debounce-ms", 100);
        List<String> trail = new ArrayList<>();
        trail.add("debounce=" + debounce + "ms");
        for (String s : enabled) {
            trail.add(s + ":onEvent(" + event + ")");
        }
```

Hot path: **local memory** after connect. Change lands as a WebSocket delta from [Kiponos.io](https://kiponos.io).

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-observer-live-bus
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Full golden example: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-observer-live-bus](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-observer-live-bus)

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
