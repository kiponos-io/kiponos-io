# We Used to Poll a File Because the Truth Would Not Knock

*A story about SIGHUP cosplay, sleep loops that pretend to be architecture, and the night a running process finally learned to hear a dashboard edit.*

---

There is a particular kind of boredom that only production engineers invent.

Not the good boredom of a quiet pager. The other kind — the one where someone has written a loop that wakes every five seconds to ask a disk the same question, forever, like a nervous traveler checking a departure board that has not changed since boarding.

I have seen that loop in more languages than I care to collect. Sometimes it was YAML. Sometimes a JSON file on a shared volume. Sometimes a flag file an ops person would `touch` like a superstition.

And when someone asked why, the answer was always almost noble:

**“So we don’t have to restart when config changes.”**

Almost noble.

Because the process still was not listening.

It was guessing on a schedule.

---

## Configuration hell wears a timer

We tell ourselves polling is “event-driven enough.”

We tell ourselves SIGHUP is a protocol. We tell ourselves that if the sidecar rewrites a file and the main process re-reads it after a nap, we have built a control plane.

I have sat with people who can design beautiful backpressure — the kind of minds that sketch queue depth on napkins between gates — reduced to arguing about poll intervals:

- Is five seconds too slow for a rate limit?  
- Is one second too chatty for the disk?  
- What if two replicas re-read at different times?  
- Who sends the signal? Who has permission to send the signal? What if the signal never arrives because the container runtime ate it?

Different cities. Same flinch.

Airports taught me that schedules are suggestions. On-call taught me that a `while(true) { sleep; reread; }` is not a nervous system. It is a **rumor mill with a clock**.

---

## What we were really avoiding

Nobody loves restarts.

Restarts are honest about their cost: drain, blip, cold caches, the quiet hope that the right artifact came back with the right env. So we invent half-measures that feel kinder:

1. Edit a file on a box  
2. Hope the process notices  
3. Or send SIGHUP  
4. Or wait for the next poll  
5. Or “just bounce one pod” when hope fails  

That theater can work until the night the limit needed to move **now** — flash traffic, a limping dependency, a partner asking for a temporary throttle — and your architecture’s answer is still *wait for the timer*.

Old world:

- Truth changes in a file or a ticket  
- The process finds out later, maybe  
- Lag is policy you never wrote down  

New world — the one this example is for:

1. Open the Kiponos hub  
2. Change `max-rps`  
3. The **running** process receives `afterValueUpdated`  
4. Posture changes without a restart, without a poll, without a signal cosplay  

Not “real-time” as a slide. Real-time as **respect for the person who already decided.**

<!-- medium-img: diagram-poll-vs-hook.png -->

---

## The example (standalone Java, hooks, no framework costume)

We published a small long-running example that refuses to treat dashboard edits as distant rumors:

**`examples/java/api-04-hooks-value-updated`** on [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

Plain `main`. No Spring ceremony. No “config refresh endpoint” you will forget to secure.

It connects to Kiponos, ensures a folder:

```text
examples / hooks-value-updated / max-rps
```

registers:

```text
kiponos.afterValueUpdated(...)
```

and keeps a worker loop that prints the live limit every few seconds.

First run can create `max-rps=50`. While it listens, you open the dashboard, change the number, and watch the hook apply it in process.

**No jar rebuild. No SIGHUP. No “wait for the next poll.”**

<!-- medium-img: diagram-hook-flow.png -->

If you want the wider catalog — kill switches, multi-env profiles, Spring Boot live beans, industry pains — it lives in the same repo under `examples/CATALOG.md`. This article is only the door marked **hooks**.

---

## A traveler’s note on listening

I have watched tools sold like souvenirs: shiny labels, useless at altitude.

The tools that matter under load are the ones that shorten the distance between **human judgment** and **running behavior**.

Polling shortens nothing. It amortizes doubt.

SIGHUP is a knock on a door that might not be home.

A value-updated hook is different. It is the process admitting that configuration is not paperwork you re-read on a schedule — it is a **conversation that can interrupt you while you are already serving traffic.**

If your language for that conversation is still “sleep and check the file,” you will keep teaching production to be late on purpose.

---

## How to try it tonight (even if nothing is on fire)

1. Free TeamPro at [kiponos.io](https://kiponos.io)  
2. Clone the public repo  
3. `cd examples/java/api-04-hooks-value-updated`  
4. Export `KIPONOS_ID` / `KIPONOS_ACCESS` from Connect  
5. `./gradlew run`  
6. In the dashboard, change `max-rps` while the process is listening  
7. Watch the hook log: old limit → new limit  

That little printout is not a demo trick. It is a rehearsal for the night the rate limit is a moral decision, not a deploy ticket.

---

## The moral, if you need one on a slide

**People should not have to ship a release to make a decision.**

And they should not have to wait for a timer to notice the decision either.

Configuration that cannot interrupt a running process will eventually interrupt customers instead.

We built Kiponos so the hub and the process can share a nervous system — including the part of the nervous system that **feels the update**, not just stores it.

This public example starts with `afterValueUpdated` because that is where honesty lives: not in architecture diagrams about “event-driven config,” but in the second a human edits a number and a worker already under load says, without drama:

*I heard you.*

---

*Code: [kiponos-io/kiponos-io — api-04-hooks-value-updated](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/api-04-hooks-value-updated)*  
*Product: [kiponos.io](https://kiponos.io)*
