---
title: "Abstract Factory as a Super Pattern — Region Family Live"
published: false
tags: java, devops, architecture, kiponos
description: "US vs EU product family without redeploy"
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-abstract-factory-live-region.md
---

**The Aha:** US vs EU product family without redeploy

You already know the knob. You already know the incident. What hurts is the **ceremony** between decision and effect.

## Config hell, one key

```yaml
# logical tree
patterns/abstract-factory/region/family: us
```

## What the process does

```java
    public static void main(String[] args) throws InterruptedException {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder policy = ensure(k);
            RegionFactory f = createFamily(policy);
            System.out.println("========================================");
            System.out.println("  Super Pattern: Live Abstract Factory");
            System.out.println("  family:   " + f.family());
            System.out.println("  currency: " + f.currency().code());
            System.out.println("  tax:      " + f.tax().label());
            System.out.println("========================================");
            Thread.sleep(2000L);
        } finally { k.disconnect(); }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("abstract-factory").folderOrCreate("region");
        if (!f.hasKey("family")) f.set("family", "us");
        return f;
    }

    static RegionFactory createFamily(Folder policy) {
        String fam = read(policy, "family", "us").toLowerCase(Locale.ROOT);
        return "eu".equals(fam) ? new EuFactory() : new UsFactory();
```

Hot path: **local memory** after connect. Change lands as a WebSocket delta from [Kiponos.io](https://kiponos.io).

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-abstract-factory-live-region
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Full golden example: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-abstract-factory-live-region](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-abstract-factory-live-region)

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
