# The Staging Config That Shipped to Prod — and Why Your Jar Was Innocent

*A story about fat jars, wrong YAML, airport wifi, and the night environment stopped being a filename.*

---

There is a special kind of dread that only appears after a “clean” deploy.

The pipeline was green. The artifact hash matched. The change log was short enough to read without coffee. And then production started calling a host that only exists in staging — politely, repeatedly, with the confidence of a process that believes it is home.

I have watched that scene in more cities than I can count on one hand: a glass office where the air conditioning always lost; a hotel lobby after a red-eye when the only open seat faced a broken fountain; once on a train platform where the network came and went like weather. Same plot. Different snacks.

Someone says the line that is almost never a joke:

**“We must have copied the wrong env file.”**

The jar was fine.

The identity was not.

---

## Configuration hell is often an identity problem

We love to talk about “config” as if it were decoration — a side salad next to the real meal of business logic.

But environments are not decoration. They are **who the process thinks it is**.

Old world theater:

- `application-dev.yml`
- `application-staging.yml`
- `application-prod.yml`
- a hope that the right one landed on the right disk
- a prayer that the merge did not “helpfully” keep the staging API base

I have seen senior engineers — people who can reason about quorum and backpressure without notes — reduced to `find / -name '*.yml'` archaeology at 11pm, because a hostname in a file became destiny.

That is not engineering. That is **file roulette with customer traffic**.

---

## What went wrong (and what did not)

The binary was correct.

The release train did its job.

What failed was the idea that **environment identity should travel as a file that can be copied wrong**.

Files are excellent at many things. They are terrible at surviving humans under pressure.

So we built a public example for the simplest honest fix:

**Same jar. Different Kiponos profile. Env-specific values live in the hub.**

Not “twelve-factor cosplay.” Not a new framework religion. A process that knows its profile path:

```text
-Dkiponos=['my-app']['v1.0.0']['dev']['base']
```

and reads:

```text
examples / multi-env / env-label
examples / multi-env / api-base-url
```

Point the same artifact at staging or prod by changing the profile — not by hoping the right YAML hitchhiked into the image.

---

## The example (standalone Java, multi-env profile)

**`examples/java/02-standalone-multi-env-profile`** on [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

Plain `main`. No Spring ceremony. No “just one more profile annotation.”

It connects with whatever profile you gave it, ensures the multi-env keys under that profile, and prints:

- the `-Dkiponos` path it believes it is  
- `env-label`  
- `api-base-url`  

First run can create safe defaults for that profile. Real teams set distinct values per environment in the dashboard.

Then you do the only exercise that matters:

1. Run with the **dev** profile  
2. Run with the **staging** profile  
3. Confirm the values do not leak  

If they do not leak, you have something files never gave you: **environment as connection identity**, not as a filename that can be fat-fingered into prod.

The longer map of app shapes and pains lives in `examples/CATALOG.md`. This article is the second door — the one after the 3am kill switch — because after you can stop the bleeding, you still have to know **which hospital you are standing in**.

---

## A traveler’s note on “it works on my machine”

Airports taught me that gates renumber without apology. Markets taught me that the same ticker can mean different nights. On-call taught me that “it works on my machine” is often a confession about **which file your machine happened to be holding**.

If your language for environment is still “copy the yml,” you will keep shipping correct jars into incorrect worlds.

The tools that matter shorten the distance between **who we intend this process to be** and **what it actually talks to**.

---

## How to try it tonight

1. Free TeamPro at [kiponos.io](https://kiponos.io)  
2. Clone the public repo  
3. `cd examples/java/02-standalone-multi-env-profile`  
4. Export `KIPONOS_ID` / `KIPONOS_ACCESS` from Connect  
5. `export KIPONOS="['my-app']['v1.0.0']['dev']['base']"`  
6. `./gradlew run`  
7. Change only `KIPONOS` to a staging or prod profile path  
8. Run again  

Watch the printed identity change.

That little block — profile path, env label, API base — is not a demo trick. It is a rehearsal for the release where the jar was innocent and the file was not.

---

## The moral, if you need one on a slide

**People should not have to ship a release to make a decision.**

And they should not have to ship a **filename** to decide who a process is.

Configuration that cannot carry environment identity at the speed of deployment will eventually carry it at the speed of incidents.

We built Kiponos so the hub and the process share a nervous system — including which room of the hospital they are in.

Example 02 is that room label, printed honestly.

---

*Code: [kiponos-io/kiponos-io — example 02](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/02-standalone-multi-env-profile)*  
*Product: [kiponos.io](https://kiponos.io)*
