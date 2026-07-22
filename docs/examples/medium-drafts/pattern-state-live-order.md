# The State Machine Was Perfect — Until We Needed to Freeze a Transition Without a Release

*A traveler’s note on GoF State, order lifecycles, and the Super Pattern that puts the transition matrix in the hub.*

---

Every order system grows a private dialect: `draft`, `paid`, `shipped`, `cancelled`.

The dialect is easy. The **grammar of what may follow what** is where nights go to die.

Someone wants to freeze `paid → cancelled` during a refund exploit. Someone else needs `draft → paid` to stay hot. The state machine is correct in code — and unreachable until the next jar lands.

---

## Super Pattern: Live State Machine

```text
patterns / state / order / current = draft | paid | shipped | cancelled
patterns / state / order / allowed = draft>paid,paid>shipped,paid>cancelled,draft>cancelled
```

`tryTransition(next)` checks the live matrix, then writes `current` when allowed.

### Snippet

```java
String edge = from + ">" + to;
if (!edges.contains(edge)) {
    return TransitionResult.denied(from, to, edge);
}
policy.set("current", to);
```

---

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-state-live-order
cp kiponos.local.env.example kiponos.local.env
./gradlew test run --args='paid'
```

Python: `examples/python/pattern-state-live-order/`

---

## The moral

**State describes where you are. Policy describes where you may go next. Policy should not wait for CI.**

---

*Example: [pattern-state-live-order](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-state-live-order)*
