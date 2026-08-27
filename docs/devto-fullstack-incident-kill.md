---
title: "Kill the Path Everywhere — JVM, Agent, React BFF, Angular Admin"
published: false
tags: java, python, kiponos, devops
description: "fullstack path kill flag live across Java, Python, React, Angular and agents — no restart."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-fullstack-incident-kill.md
main_image: https://iili.io/CbUdd8u.jpg
---

**The Aha:** fullstack path kill flag is not a properties-file trophy. It is **mesh posture** — and posture that waits for four redeploys is already late.

We killed the Java route. The Python agent kept writing. The admin UI still offered the button.

Someone said:

> A kill switch that leaves three peers live is not a kill switch. It is a rumor.

Domain: multi-SDK distributed systems, agents, MCP, observability, war-room truth. This essay maps hub key `path-enabled` under `examples/fullstack-incident-kill/` so the lesson stays concrete.

## The problem: ceremony between judgment and effect

You already know the right value for **fullstack path kill flag**. The war room knows. The agent almost knows. What you do not have is a path from **mouth → every running peer** that is shorter than a release train and safer than pasting tokens into a SPA.

When `path-enabled` is frozen in YAML (or baked into a browser build), every incident becomes a process argument. When it lives in a hub with clamps, the argument ends and the work begins — for Java services, Python agents, React BFFs, and Angular admin hosts **at once**.

| Belief | Production |
|--------|------------|
| Flags cover this | Second system, second delay, second outage mode |
| Restart the agent | Sessions die; context dies; the bug changes |
| Put the SDK in the frontend | Tokens leak or defaults lie |
| GitOps will handle it | Git is a ledger, not a pager for second-scale posture |
| We'll tune next sprint | Incidents do not respect sprints |

## The Aha: local read, live write, multi-peer

[Kiponos.io](https://kiponos.io) holds the tree. Each **SDK** keeps the latest value **in memory**, patched over WebSocket deltas. Hot path: **local get** — no per-request hub RTT.

Public SDKs today:

| SDK | Role |
|-----|------|
| **Java** | Services, workers, Spring Boot 2/3 |
| **Python** | Agents, workers, automation |
| **React (`@kiponos/react`)** | **Node server** peer — SPA mirrors via your API/SSE |
| **Angular (`@kiponos/angular`)** | **Node server** peer — same moral |

Agents and MCP hosts are peers too: skill sets, tool allow-lists, token budgets, handoff tickets, and debug probes are leaves — **not** restart reasons.

```yaml
examples/
  fullstack-incident-kill/
    path-enabled: on   # fullstack path kill flag
    hardMax: compiled-in-app
    failClosed: true
```

## Architecture

![Architecture diagram](https://iili.io/CSnvfNs.png)

## Config tree (this story)

```text
examples / fullstack-incident-kill
  path-enabled = on
  reason = incident-ticket
  set-by = oncall
  set-at = <iso>
```

Profile path shape: `['my-app']['v1.0.0']['dev']['base']` — use your real Connect profile.

## Integration — Java hot path

```java
Kiponos kip = Kiponos.createForCurrentTeam();
Folder f = kip.getRootFolder()
    .folderOrCreate("examples")
    .folderOrCreate("fullstack-incident-kill");
String posture = f.hasKey("path-enabled")
    ? f.get("path-enabled")
    : "on";
// honor posture on the hot path — no hub RTT
```

Runnable: [examples/java/fullstack-incident-kill](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/fullstack-incident-kill)

## Integration — Python agent (no session kill)

```python
from kiponos import Kiponos
k = Kiponos.connect(quiet=True)
# Read examples/fullstack-incident-kill/path-enabled live.
# Enabling a skill, tightening a budget, or pausing MCP writes
# must not require killing this process.
```

Companion: [examples/python/fullstack-incident-kill](https://github.com/kiponos-io/kiponos-io/tree/master/examples/python/fullstack-incident-kill)

## Integration — React / Angular server peers

```ts
import { Kiponos } from '@kiponos/react/server';
// Angular: @kiponos/angular — same createFromEnv moral
const kip = Kiponos.createFromEnv();
await kip.connect();
// set/get under examples/fullstack-incident-kill — inject mirror to SPA; never embed Connect tokens in bundles
```

## Real scenarios

| Event | Without live mesh | With Kiponos multi-SDK mesh |
|-------|-------------------|-----------------------------|
| Kill a bleeding path | Kill Java only; UI/agent keep going | One leaf; all honest peers stop |
| Enable agent skill | Restart agent; lose context | Skill set leaf; session stays |
| Need 100% traces for 12 min | Four YAML edits + redeploys | `sample-percent` live |
| Degrade mode | Services quiet; agents still hammer | SRE leaf pauses agent tools |
| War-room status | Slack vs dashboard vs agent | One `headline` leaf |
| Canary 10% | API only; SPA/agent wrong | Shared percent leaf |

## Performance notes (this use case)

- Hot path reads are **local memory** after bootstrap — mesh does not add hub RTT per request.  
- Deltas fan out to every connected peer; agents do not poll.  
- Browser cost is your SSE/API, not Connect chat from the SPA.  
- Fail-closed clamps keep money/kill paths safe if the hub is dark.  
- Multi-peer alignment removes “which tier still has the old value?” thrash.

## Compare to alternatives

| Approach | Honest fit | Gap |
|----------|------------|-----|
| Env YAML + redeploy | Versioned defaults | Too slow for incidents |
| Redis pub/sub DIY | Possible | You rebuild the control plane |
| Feature-flag SaaS | Product experiments | Often not agent/MCP/session-safe |
| “SDK in the SPA” | Tempting | Secrets + lies |
| **Kiponos multi-SDK mesh** | Ops + agents + BFF peers | Not a secrets vault; not GitOps ledger |

## When not to use Kiponos

| Situation | Prefer |
|-----------|--------|
| Cryptographic secrets | Vault / KMS |
| Schema migrations | Versioned deploy |
| One-shot offline batch with no peer | Local config may suffice |
| Product A/B for anonymous web visitors | Product experiment platform |

## Getting started (15 minutes)

1. Create a free hub profile on [kiponos.io](https://kiponos.io) → Connect → copy `KIPONOS_ID` / `KIPONOS_ACCESS` / profile.  
2. Clone [kiponos-io](https://github.com/kiponos-io/kiponos-io).  
3. `cd examples/java/fullstack-incident-kill && cp kiponos.local.env.example kiponos.local.env` and fill tokens.  
4. `./gradlew test run` — print `path-enabled`.  
5. Flip the leaf on the dashboard; re-run or leave a long-lived peer connected — no rebuild.  
6. Optional: `examples/python/fullstack-incident-kill` agent peer; `@kiponos/react` / `@kiponos/angular` on Node.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

## Closing

**fullstack path kill flag** belongs on the hub — not in four redeploys, not in a dead agent session, not in a browser secret. The multi-SDK mesh is here. Use it while the process still runs.
