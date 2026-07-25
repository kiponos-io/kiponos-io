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

<!-- medium-img: diagram-pattern-state-live-order-gof-vs-live.png -->
<!-- medium-img: diagram-pattern-state-live-order-hub-flow.png -->


---

## Why this still matters on a quiet Tuesday

Incidents train the muscle. Quiet days keep it honest.

If `draft` only moves through a release train, every product conversation becomes a ceremony debate: who owns the PR, who merges, who rolls, who watches. That tax compounds across regions and on-call rotations.

Live posture is not a license for chaos. It is a contract:

- named path humans can find under pressure  
- clamps that survive panic  
- audit that names the actor  
- a one-line revert that does not require a hero  

When those four exist, the Super Pattern stops being a demo and becomes how the system grows older without growing brittle.


## Operational checklist (keep this boring)

1. Name the hub path so humans find it under pressure.  
2. Default safely when the hub is unreachable.  
3. Allowlist writers (dashboard roles + automation identities).  
4. Log the **decision**, not every get.  
5. Rehearse the flip in staging.  
6. Document the one-line kill path (revert key).  
7. Record from→to + reason code in the incident timeline.

Boring checklists survive 3am. Clever ones do not.

