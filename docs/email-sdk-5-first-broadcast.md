# First email to Kiponos users — SDK 5.0

**Context:** ~1,000 signed-up users; **first product email ever** (high trust / high stakes).  
**Goals:** announce 5.0 · reopen onboarding · feel moral restraint · one-click *experience*, not a lecture.

---

## Strategy (why this shape)

| Principle | Application |
|-----------|-------------|
| **Earn the open** | Subject is short, concrete, non-hype |
| **Respect silence** | Acknowledge you never emailed them — once, lightly |
| **One job** | Primary CTA: *experience Kiponos in minutes* (game or golden) |
| **5.0 is the news** | Reliability modes + Maven coords as proof of maturity |
| **Incomplete onboarding** | Don’t shame; offer the shortest path to “aha” |
| **Moral capital** | Restraint = brand: we only interrupt for something real |

**Do not** dump full release notes in email. Link out.

---

## Subject lines (pick one)

**Recommended:**

> **Kiponos 5.0 is live — try it in one click**

Alternates:

> Your first email from us: Java SDK 5.0  
> Kiponos 5.0 on Maven Central (we saved the inbox until it mattered)  
> 5.0 — live config, calmer apps  

**Preheader (inbox preview):**

> Ready / Offline / Safe modes + Last Known Good. Play the demo or run the golden sample.

---

## Body A — “one liner” (SMS-short)

Use if your ESP supports very short campaigns.

**Subject:** Kiponos 5.0 is live — try it in one click  

**Body:**

> Kiponos Java **SDK 5.0** is on Maven Central — our first email to you, because this one matters.  
>  
> **→ Feel it now:** [Play the live demo game](https://github.com/Avdiel/kiponos-game)  
> **→ Or ship it:** `implementation 'io.kiponos:sdk-boot-3:5.0.0.260710'`  
>  
> Signed up long ago and never finished setup? Start here in ~5 minutes: [golden Java sample](https://github.com/kiponos-io/kiponos-io/tree/master/golden/java) · [your Connect tokens](https://kiponos.io)  
>  
> Thank you for your patience while we stayed quiet.  
> — Moshe, Kiponos

*(Adjust Connect URL to the real dashboard login / connect path you use.)*

---

## Body B — short letter (recommended default)

**Subject:** Kiponos 5.0 is live — try it in one click  

Hi,

This is the **first email** we’ve sent since you signed up for Kiponos.

We stayed quiet on purpose — no drip noise, no “just checking in.” We’re writing today because **Java SDK 5.0** is live on Maven Central, and it’s the release that makes live config feel as solid as it does delightful.

### What’s new (30 seconds)

- **Ready** — full live WebSocket config  
- **Offline** — keep reading **Last Known Good** when the wire blips  
- **Safe** — fail closed when nothing is trustworthy  

```text
implementation 'io.kiponos:sdk-boot-3:5.0.0.260710'
```

### One-click paths (pick one)

| If you want… | Do this |
|--------------|---------|
| **To feel it immediately** | Open the **[Kiponos Game](https://github.com/Avdiel/kiponos-game)** (libGDX demo driven by the live dashboard) |
| **To finish onboarding** | Run the **[golden Java sample](https://github.com/kiponos-io/kiponos-io/tree/master/golden/java)** with tokens from Connect |
| **To read the story** | [Happy 5.0 announcement](https://dev.to/kiponos/…) · [Developer What’s New](https://dev.to/kiponos/…) |

Many of you joined from an ad, created an account, and never got a clean first run. That’s on us to make easier — **start with the game or the golden sample**; the dashboard makes sense the moment something moves live.

Thank you for the trust of a quiet inbox.

— Moshe  
Kiponos.io

---

## Body C — ultra-minimal (button-first HTML sketch)

```
[Logo]

Kiponos 5.0 is on Maven Central.

Our first email to you — only because this release is worth it.

[  Play the live demo  ]   ← primary, large
[  Run 5-minute sample ]   ← secondary

Maven: io.kiponos:sdk-boot-3:5.0.0.260710

We never emailed you before. We still won’t spam you.
```

---

## “One click” reality check (product + marketing)

True single-click install for a signed-up user needs **one** of:

1. **Visceral demo (best for cold incomplete onboarding)**  
   - Public game repo + short README “clone → tokens → run”  
   - Even better later: hosted playable build (web/desktop) — highest conversion  

2. **Public sandbox tokens** (read-only shared hub)  
   - Zero account friction for the *feel*; then “claim your own tree”  
   - Aligns with your planned PUBLIC-SANDBOX work  

3. **Deep link into Connect** with checklist:  
   `Create token → copy profile → open golden`  

4. **Magic onboarding link** (future):  
   email token → pre-provision sample env → one button “Open dashboard with sample tree”

**For this send:** Game + golden Java + Connect is the honest “as close as one click as open source allows.”  
Lead with **game** for emotion; golden for engineers who already know Gradle.

---

## Moral line (use once, don’t sermonize)

> We never emailed you before. We’re not about to start a weekly newsletter.  
> When we write, it’s because something real shipped.

That positions **you** as the product’s character: restraint = care = long-term service.

---

## Segmentation (if ESP allows)

| Segment | CTA |
|---------|-----|
| Never generated SDK tokens | Game + “open Connect” |
| Generated tokens, never ran SDK | Golden Java with their account |
| Active / recent login | 5.0 upgrade coords + What’s New guide |
| Very old / dormant | Shortest Body A + game only |

If **no segments:** use Body B for everyone.

---

## Compliance / hygiene

- Clear unsubscribe (legal + brand)  
- From: personal-ish `moshe@kiponos.io` or `hello@` with Moshe signature  
- Reply-to monitored (first email will get real replies — treat as gold)  
- Don’t attach large files; links only  
- Don’t mix billing asks  

---

## After send

1. Publish both dev.to articles + Crunchbase  
2. Pin game + golden in GitHub profile / kiponos-io README  
3. Track: open rate, CTA clicks (game vs golden vs Maven)  
4. Replies → personal onboarding help (high-touch still scales at 1k if only 2–5% reply)

---

## Fill-in before send

- [ ] Final Connect / login URL  
- [ ] Final dev.to URLs after publish  
- [ ] Optional: hosted GIF of game for HTML email  
- [ ] Confirm game stays public + README has 5-minute run  
