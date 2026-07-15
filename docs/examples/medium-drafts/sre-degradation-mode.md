# The Night We Needed a Read-Only Button — and GitHub Was the Only One We Had

*A story about limping dependencies, temporary PRs that become permanent debt, and the SRE judgment that should not wait for a release train.*

---

There is a particular kind of clarity that arrives only when something is half-broken.

Not fully down. Not fully fine. The middle state — the one where reads still answer, support still has a UI to stare at, and every write feels like pouring water into a cracked glass.

I have met that state in hotel lobbies after red-eyes, in war rooms that smell like cold pizza, and once on a balcony where the city was too loud for panic and too quiet for sleep. Different clocks. Same sentence from the person holding the pager:

**“We need read-only mode. Now.”**

Not a redesign. Not a new architecture. A button.

And then someone opens the repo.

---

## Configuration hell for people who already know the answer

SRE judgment is often simple under stress.

Stop accepting mutations. Park the expensive jobs. Keep the process alive so you can still see what is true. Let the dependency catch its breath — or let humans catch theirs.

The hard part is rarely the decision. The hard part is the **distance between the decision and the running system**.

Old world theater:

1. Find the flags (they are never in one file)  
2. Open a PR titled `temp: enable read-only during incident`  
3. Wait for CI like it is weather  
4. Deploy  
5. Discover one replica is still on yesterday’s artifact  
6. Merge a “revert the temporary thing” PR three days later that nobody reviews carefully  

I have watched excellent engineers — people who can reason about error budgets without notes — reduced to archaeologists of their own YAML because “safer posture” was packaged as a release.

That flinch is configuration hell wearing an on-call vest.

Airports taught me that gates renumber without apology. On-call taught me that a temporary PR is a confession: **your control plane is your git history.**

---

## What the degradation tree is really about

This is not about a single boolean named `readOnly`.

A real service has a small family of related moves — a **kill-switch tree** under one mental folder:

- What mode are we in?  
- Do we still accept writes?  
- Do background jobs keep chewing the dependency?

Old world: those answers live in three files, two env overlays, and a wiki page last edited during a previous fire.

New world — the one this example is for:

1. Open the Kiponos hub  
2. Set `mode` to `read-only` (or `maintenance`)  
3. The process can adopt a safer posture without a parade of pull requests  

Not “feature flags as a product tour.”  
Judgment that reaches machines **at the speed of the person who is already awake.**

<!-- medium-img: diagram-read-only-old-vs-new.png -->

---

## The example (standalone Java, no framework costume)

We published a small example for that button:

**`examples/java/sre-degradation-mode`** on [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

Plain `main`. No Spring ceremony. No “config refresh endpoint” you will forget to secure at 4am.

It connects to Kiponos and ensures a tree:

```text
examples / sre-degradation-mode /
  mode              = full | read-only | maintenance
  accept-writes     = yes | no
  background-jobs   = yes | no
```

Then it prints the **effective posture**:

| Mode | Writes | Background jobs |
|------|--------|-----------------|
| `full` | honor the knobs | honor the knobs |
| `read-only` | forced off | still your call |
| `maintenance` | forced off | forced off |

First run can create defaults (`mode=full`, writes and jobs on). Next runs honor the hub.

Ops flips the dashboard. You re-run. **No jar rebuild.** No “temporary” branch that becomes institutional memory.

<!-- medium-img: diagram-degradation-tree.png -->

If you want the longer catalog of app shapes and industry pains — Spring Boot, Kafka workers, kill switches at 3am, multi-env identity — it lives in the same repo under `examples/CATALOG.md`. This article is the SRE door.

---

## A traveler’s note on “temporary”

I have learned to distrust the word *temporary* in production.

Temporary is how snowflakes become permanent climate. Temporary is how a Friday hotfix becomes the only documentation of an incident. Temporary is how a boolean in a PR title becomes the reason nobody knows whether writes are allowed on Sunday.

The tools that matter under stress are the ones that let **policy change without a costume change** — without dressing a human decision up as a release artifact.

Markets taught me that prices are moods. On-call taught me that posture is not paperwork. It is **how an organization tells its machines to stop digging while the ground is still shaking.**

If your language for that speech is still “open a PR and redeploy,” you will keep spending error budget on orthography.

---

## How to try it tonight (even if nothing is limping)

1. Free TeamPro at [kiponos.io](https://kiponos.io)  
2. Clone the public repo  
3. `cd examples/java/sre-degradation-mode`  
4. Export `KIPONOS_ID` / `KIPONOS_ACCESS` from Connect  
5. `./gradlew run`  
6. Set `mode` to `read-only` in the dashboard  
7. Run again  

Watch the posture change.

That little printout — FULL vs READ-ONLY vs MAINTENANCE — is not a demo trick. It is a rehearsal for the night the dependency is half-broken and the only thing that should be hard is the diagnosis — not the button.

---

## The moral, if you need one on a slide

**People should not have to ship a release to make a decision.**

Degradation is not failure theater. It is responsible posture.

Configuration that cannot move at the speed of judgment will eventually move at the speed of incidents — and then of postmortems that all say the same thing: *we knew, we just could not flip it fast enough.*

We built Kiponos so the hub and the process can share a nervous system.

And we put the public SRE example on a degradation tree because honesty lives there: not in perfect uptime slides, but in the hour when the system is still breathing and the right move is to stop making it work harder.

---

*Code: [kiponos-io/kiponos-io — sre-degradation-mode](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/sre-degradation-mode)*  
*Product: [kiponos.io](https://kiponos.io)*
