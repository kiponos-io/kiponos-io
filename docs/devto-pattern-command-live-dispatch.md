---
title: "Command Super Pattern — Dry-Run and Enable Live"
published: false
tags: java, devops, architecture, kiponos
description: "block or dry-run commands live"
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-command-live-dispatch.md
---

**The Aha:** block or dry-run commands live

You already know the knob. You already know the incident. What hurts is the **ceremony** between decision and effect.

## Config hell, one key

```yaml
# logical tree
patterns/command/dispatch/refund.enabled: yes
```

## What the process does

```java
    public static void main(String[] args) throws Exception {
        String cmd = args.length > 0 ? args[0] : "refund";
        long amount = args.length > 1 ? Long.parseLong(args[1]) : 500L;
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println(dispatch(p, cmd, amount));
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("command").folderOrCreate("dispatch");
        if (!f.hasKey("refund.enabled")) f.set("refund.enabled", "yes");
        if (!f.hasKey("refund.dry-run")) f.set("refund.dry-run", "no");
        return f;
    }
    static String dispatch(Folder policy, String cmd, long amount) {
        String c = cmd.toLowerCase(Locale.ROOT);
        if (!truthy(read(policy, c + ".enabled", "yes"))) return "blocked: disabled " + c;
        boolean dry = truthy(read(policy, c + ".dry-run", "no"));
        return (dry ? "DRY-RUN " : "EXEC ") + c + " amount=" + amount;
    }
    static boolean truthy(String s) {
        return s != null && (s.equalsIgnoreCase("yes")||s.
```

Hot path: **local memory** after connect. Change lands as a WebSocket delta from [Kiponos.io](https://kiponos.io).

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-command-live-dispatch
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Full golden example: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-command-live-dispatch](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-command-live-dispatch)

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
