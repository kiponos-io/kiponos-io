# The Factory Still Built Objects — We Just Stopped Redeploying to Choose Which One

*A traveler’s note on Factory Method, notification channels, and the Super Pattern that makes `product` a live hub value.*

---

Factory Method promises: **defer which class to instantiate.**

In most codebases, “defer” means “until the next release.” Email is hard-coded. Slack is a weekend branch. SMS is a comment that says `// TODO`.

An incident needs pages in Slack **now**, not after pipeline green.

---

## Super Pattern: Live Product Factory

```text
patterns / factory / notify / product    = email | sms | push | slack
patterns / factory / notify / from-email = string
patterns / factory / notify / slack-hook = string
```

### Snippet

```java
Notifier n = switch (read(policy, "product", "email")) {
    case "sms" -> new SmsNotifier();
    case "slack" -> new SlackNotifier(hook);
    default -> new EmailNotifier(from);
};
```

---

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-factory-live-channel
./gradlew test run --args='Warehouse delay on order 9'
```

Python: `examples/python/pattern-factory-live-channel/`

---

## The moral

**Factories create objects. Super factories create the right object for the next minute of production.**

---

*Example: [pattern-factory-live-channel](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-factory-live-channel)*
