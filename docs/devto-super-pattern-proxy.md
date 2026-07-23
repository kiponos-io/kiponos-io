---
title: "Proxy That Can Close the Door Without a Deploy (Kiponos Super Patterns)"
published: true
tags: java, designpatterns, security, devops
description: Live enable/deny, rate, and role allow-list for sensitive paths — GoF Proxy as a Super Pattern with Kiponos.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-super-pattern-proxy.md
main_image: https://files.catbox.moe/9rxq3v.jpg
---

**The Aha:** A proxy that cannot change policy at 2am is just another hardcoded gate. Super Proxies keep the door code stable while **who may enter** lives in the hub.

I have watched admin APIs that were “protected” by a proxy class with more comments than tests.

```java
if (!user.hasRole("admin")) throw new Forbidden();
return realAdminApi.invoke(req);
```

That line looks responsible. It is also a **fossil** the moment you need:

- temporary break-glass for an on-call role  
- a lower rate limit while an abuse wave rolls  
- a full close of a sensitive path without waiting for CI  

Security theater loves static proxies. Production needs a **door that can close from the control room**.

## The problem: access policy packaged as a jar

Gang of Four Proxy controls access to another object. Fine.

What we ship instead:

| Belief | Production |
|--------|------------|
| “We have a proxy, so we are safe” | Policy is compile-time |
| “Roles never change mid-incident” | Roles change the minute something is on fire |
| “Rate limits belong in the gateway only” | App-level sensitive paths still need a kill switch |
| “Feature flags cover this” | Second system, second delay |

The proxy pattern was never wrong. **Burying enable/deny/rate/allow-list in bytecode** was.

I have heard the midnight sentence more than once:

**“Disable the admin export endpoint. We will ship a hotfix.”**

While the endpoint is still open. While the jar is still the same. While the door is a compile-time constant.

## The Aha: Proxy + live access policy = Super Pattern

Keep the GoF shape — a surrogate that guards the real subject.

Move the **policy** into [Kiponos.io](https://kiponos.io):

```yaml
patterns/
  proxy/
    admin-api/
      enabled: yes                 # yes | no  — hard close
      rate-per-min: 30
      role-allow: operator,admin   # csv
```

Hot path (local reads after connect):

```java
Folder policy = kiponos.path("patterns", "proxy", "admin-api");
if (!policy.getBoolean("enabled")) {
    throw new ServiceUnavailable("admin-api closed by hub policy");
}
Set<String> allow = parseCsv(policy.getString("role-allow"));
if (!allow.contains(user.role())) {
    throw new Forbidden("role not in live allow-list");
}
if (!rateLimiter.tryAcquire(policy.getInt("rate-per-min"))) {
    throw new TooManyRequests();
}
return realAdminApi.invoke(req);
```

Ops sets `enabled=no`. The next request hits a closed door. No redeploy. No “we will hotpatch after the pipeline.”

Remote automation — including other Kiponos SDK clients — can write the same keys when your runbook says so. Humans and machines share one tree.

## What the proxy still owns in code

| Code (versioned) | Hub (live) |
|------------------|------------|
| AuthN plumbing | enabled |
| Subject invocation | role allow-list |
| Metrics / audit log lines | rate-per-min |
| Error types | temporary break-glass roles |

Do **not** put cryptography choice or token verification algorithms solely in a dashboard without process. Do put **whether this sensitive path is open tonight**.

## Architecture

```text
Client ──▶ AccessProxy ──▶ RealAdminApi
               │
               │  local get()
               ▼
         Kiponos SDK cache
               ▲
               │ WebSocket deltas
         Kiponos.io hub  (ops / automation)
```

The proxy is the door. The hub is the lock board.

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-proxy-live-access
cp kiponos.local.env.example kiponos.local.env
./gradlew test run --args=operator
```

Runnable: [pattern-proxy-live-access](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-proxy-live-access)

Try it tonight:

1. Run with `operator` — should pass when allow-list includes it.  
2. Set `enabled=no` in the hub — next call is closed without a rebuild.  
3. Tighten `rate-per-min` — prove the limiter reads live.  
4. Ask: how many minutes would a hotfix take in your org?

## Scenarios

| Moment | Frozen proxy | Super Pattern |
|--------|--------------|---------------|
| Abuse on export | Ship deny | `enabled=no` now |
| Break-glass for contractor | Emergency PR | Temporary role in `role-allow` |
| Noisy neighbor | Redeploy rate | `rate-per-min` live |
| Post-incident reopen | Another ship | `enabled=yes` |

## When not to live-edit policy

- Permanent security model changes that need review boards  
- Multi-region legal freezes that must be auditable offline  
- Anything you cannot afford to flip by mistake without two-person control  

Super Proxy is for **operational access posture**, not for skipping governance.

## Moral

Close the door from the hub. Leave the jar alone.

If your proxy cannot change its mind without a release, it is not protecting production — it is protecting yesterday’s assumptions.

---

*Series: Kiponos Super Patterns. Intro: [Rewriting the Gang of Four](https://dev.to/kiponos/rewriting-the-gang-of-four-true-real-time-config-turns-design-patterns-into-super-patterns-nii).*

*Runnable: [pattern-proxy-live-access](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-proxy-live-access) · [kiponos.io](https://kiponos.io)*

## SDK note

Kiponos ships **Java** (Spring Boot 2 and 3 patterns) and **Python** SDKs. This essay uses the Java example; the same hub tree is readable from Python workers with the same local-get / WebSocket-delta model. Do not invent a third language SDK for this pattern — if you are not on Java or Python, keep selection in your existing control plane and treat this as the design target.

