# Bridge Super Pattern — Implementor Chosen Live

*A traveler’s note: smtp|ses|sendgrid selection live.*

---

There is a class of production decisions that are **too small for a release** and **too important for a wiki**.

smtp|ses|sendgrid selection live

Redeploying a jar to change `implementor` is how teams invent 3am folklore.

---

## Hub tree

```text
patterns/bridge/notify/implementor = smtp
```

Local `get()` on the hot path. Dashboard or remote SDK `set()` when the world changes.

---

## Snippet

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

---

## Clone and run the full golden example

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-bridge-live-implementor
cp kiponos.local.env.example kiponos.local.env   # tokens from kiponos.io → Connect
./gradlew test run
```

Full source + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-bridge-live-implementor](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-bridge-live-implementor)

This article only shows the nerve. The repo is the product.

---

## Old world vs Kiponos

| Move | Old world | Live hub |
|------|-----------|----------|
| Change the knob | PR → CI → roll | Dashboard / SDK `set()` |
| Wrong replica | Drift | Same tree, WebSocket fan-out |
| Incident rollback | Redeploy previous | Flip the value back |

---

## The moral

**People should not have to ship a release to make a decision.**

Ship the judgment path once. Leave the jar alone.

---

*Example + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-bridge-live-implementor](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-bridge-live-implementor)*
