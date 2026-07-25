# The Chain of Responsibility Pattern Reorder Handlers Live — Insurance War Room

*A traveler’s note from the claims floor: velocity fraud, frozen YAML, and the Super Pattern that lets ops move **reorder handlers live** without a jar.*

---

At **03:41**, the insurance war room already knew the number. The process still believed last week’s properties file.

Someone said the sentence that always costs a night:

**“Flags cover this — we’ll cut a PR.”**

A PR. To change a number the business already decided verbally. CI still green. Attackers (or customers, or the bill) not waiting.

---

## What we thought we bought

Classic structure is not the enemy. Strategy, Decorator, Chain, Factory — GoF still names the shape of good software.

What the frozen form steals is **time**. The time between a human judgment and a running process that obeys it. When that gap is longer than the incident, architecture becomes ceremony.

For insurance, the painful dial was **reorder handlers live** (`active`). Default in the jar: `baseline`. Correct for a demo. Wrong for a brownout.

---

## Super Pattern: live posture on a hot path

Keep the code path. Move the number (or the active id / chain order) into [Kiponos.io](https://kiponos.io):

```text
patterns / chain / insurance
  active = baseline
```

- Every decision uses **local** `get()` after connect — no hub RTT on the hot path  
- Dashboard or remote SDK can change `active` while this process keeps running  
- Offline / dark hub: fail closed on money paths; fail open only where product policy says so  
- Audit actor, old, new, ticket — live is not anonymous  

**Structure stays classical. Selection becomes operational.**

<!-- medium-img: diagram-medium-chain-insurance-live-gof-vs-live.png -->

---

## The night velocity fraud met a fossil

I have been in rooms where the dependency was already sick and the client stayed “resilient”: more retries, longer open-wait, wider canary — all fossils in the image.

Resilience that cannot be steered is not resilience. It is a thrash amplifier with good intentions.

With a live hub path, ops can:

1. Confirm the signal (SLO burn, partner errors, queue depth).  
2. Move `active` to the emergency value (documented floor/ceiling).  
3. Watch two metrics for five minutes.  
4. Step or revert.  
5. Write from→to + reason in the timeline.

No second product. No SSH folklore. Same tree in every region; values differ on purpose.

---

## The example

```bash
examples/java/medium-chain-insurance-live
./gradlew test run
```

It prints the live `active` and a one-line decision trace. Change the hub. Run again. The jar is innocent.

Unit tests pin structure and clamps without tokens. Golden tests skip cleanly when `KIPONOS_ID` is a placeholder.

<!-- medium-img: diagram-medium-chain-insurance-live-hub-flow.png -->

---

## Scenarios

| Moment | Frozen YAML | Live hub |
|--------|-------------|----------|
| Incident | PR + pipeline | Seconds |
| Peak event | Over-provision | Dial down/up |
| Experiment | Long-lived branch | Same jar |
| Rollback | Redeploy previous | Revert hub value |
| Region skew | Copy three files | Per-folder values |

---

## What never goes live without review

Protocol/schema changes, crypto material, legal freezes stay in code review. Posture numbers war rooms already shout belong in the hub with clamps and allowlisted writers.

Do not put secrets in the ops tree. Do not use live knobs as a substitute for fixing a flaky dependency. Do use them when the cost of a wrong number *tonight* is higher than a controlled live edit.

---

## Observability you actually need

Ship counters with the key path baked in: decisions applied, rejects, and **hub write events**. Logging every local get teaches nothing; logging every change teaches ownership.

---

## Moral

Reorder handlers live that requires a deploy is optimistic documentation.

People should not have to ship a release to make a decision the business already made in a sentence.

---

*Series: Kiponos Medium Super Patterns · Example: [`examples/java/medium-chain-insurance-live`](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/medium-chain-insurance-live) · Product: [kiponos.io](https://kiponos.io)*
