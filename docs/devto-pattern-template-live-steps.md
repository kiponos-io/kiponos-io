---
title: "Template Method Super Pattern — Optional Steps Live"
published: false
tags: java, devops, architecture, kiponos
description: "pipeline steps toggled from hub"
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-template-live-steps.md
---

**The Aha:** pipeline steps toggled from hub

You already know the knob. You already know the incident. What hurts is the **ceremony** between decision and effect.

## Config hell, one key

```yaml
# logical tree
patterns/template/onboard/steps: validate,enrich,persist,notify
```

## What the process does

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("steps=" + runTemplate(p, "user-1"));
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("template").folderOrCreate("onboard");
        if (!f.hasKey("steps")) f.set("steps", "validate,enrich,persist,notify");
        return f;
    }
    static List<String> runTemplate(Folder policy, String id) {
        List<String> steps = new ArrayList<>();
        for (String s : read(policy, "steps", "validate,enrich,persist,notify").split(",")) {
            String t = s.trim().toLowerCase(Locale.ROOT);
            if (t.isEmpty()) continue;
            steps.add(t + "(" + id + ")");
        }
        return steps;
    }
    static String read(Folder p, String k, String d) {
        if (!p.hasKey(k)) return d;
```

Hot path: **local memory** after connect. Change lands as a WebSocket delta from [Kiponos.io](https://kiponos.io).

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-template-live-steps
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Full golden example: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-template-live-steps](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-template-live-steps)

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
