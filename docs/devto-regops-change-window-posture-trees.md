---
title: "Change Window Posture Trees — Live Freeze Flags for Regulated Releases (Java SDK)"
published: false
tags: java, compliance, devops, architecture
description: Change freeze flags scattered across pipelines. Kiponos centralizes freeze posture — ops flips without infra PR, not compliance certification.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-regops-change-window-posture-trees.md
main_image: https://files.catbox.moe/574r3o.jpg
---

Thursday 08:00 UTC. **Audit week** begins — external assessors arrive Monday. Release management emails a freeze memo: no production mutations except break-glass. Yet `CHANGES_ALLOWED = true` still gates your admin mutation API because the flag lives in `release-freeze.yml` last touched during holiday code freeze.

Three teams still push config mutations through internal tools. The regulated-release lead asks:

> "We need **`changes_allowed: false`** live across **twelve services** — not a **Terraform PR** that lands after lunch. Why is freeze posture trapped in bootstrap YAML?"

**Honest framing:** Kiponos centralizes **`changes_allowed`** and related freeze flags for **operational change posture**. It helps ops enforce "no prod mutations during audit week" consistently. It does **not** replace your formal change-advisory board, SOC2 change-management narrative, or compliance certification. The tree holds **whether mutations proceed right now** — not your entire ITIL program.

## The problem: changes_allowed scattered and static

```java
@RestController
public class AdminMutationController {
    private static final boolean CHANGES_ALLOWED = true;

    @PostMapping("/admin/mutations")
    public ResponseEntity<?> applyMutation(@RequestBody MutationRequest req) {
        if (!CHANGES_ALLOWED) {
            return ResponseEntity.status(423).body("change freeze active");
        }
        return ResponseEntity.ok(mutationService.apply(req));
    }
}
```

Freeze flags in three places — pipeline env, YAML, and constants — none synchronized:

```yaml
release:
  prod:
    freeze:
      changes_allowed: true
```

During audit week you need to:

1. Set **`freeze.changes_allowed`** to `false` globally
2. Allow **`exceptions.break_glass_team_ids`** for platform SRE only
3. Enable **`notify.slack_freeze_channel`** on flip

Recycling twelve services to flip one boolean is **freeze theater** — mutations slip through services that restarted slower.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Freeze is a pipeline concern" | Internal admin APIs bypass CI freeze env vars |
| "We'll email engineers to stop" | Email does not gate `applyMutation()` |
| "Terraform plan-only mode is enough" | App-layer mutations still proceed |
| "One global env var works" | Twelve JVMs boot at different times — drift |
| "Audit week ends Friday 5pm" | Ops needs instant unfreeze Saturday without deploy |

## The Aha

**`changes_allowed` is operational config** — it flips for audit weeks, holiday freezes, and post-incident stabilization. It belongs in profile `['release']['prod']['freeze']` with local `getBool()` on every mutation gate.

## What Kiponos.io is for change window posture (RegOps)

[Kiponos.io](https://kiponos.io) shares one freeze tree across all mutation-gated services. Profile `['release']['prod']['freeze']` hydrates every JVM; one dashboard edit propagates via WebSocket delta.

`afterValueChanged` notifies Slack, logs ACL evidence, and increments `freeze_posture_change_total`.

**RegOps boundary:** Kiponos proves **who enabled freeze and when** — supporting change-management evidence. It does **not** certify SOC2 or replace CAB minutes.

## Reference architecture

![Architecture diagram](https://files.catbox.moe/1sjl5s.png)

## Config tree — freeze, exceptions, notify, scopes, meta

Five folders — `freeze`, `exceptions`, `notify`, `scopes`, `meta`:

```yaml
freeze/
  changes_allowed: true
  freeze_reason: ""
  freeze_started_at_ms: 0
  auto_expire_at_ms: 0
exceptions/
  break_glass_team_ids: ["platform-sre"]
  break_glass_max_mutations_per_hour: 10
  require_ticket_id: true
notify/
  slack_freeze_channel: "#release-freeze"
  pager_on_unauthorized_mutation: true
  email_release_mgmt: true
scopes/
  block_admin_mutations: true
  block_schema_migrations: true
  block_feature_flag_writes: false
meta/
  last_change_by: ""
  siem_forward_enabled: true
```

Profile path: `['release']['prod']['freeze']`.

## Java integration: live freeze gate + afterValueChanged

```java
import io.kiponos.sdk.Kiponos;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class RegOpsMutationController {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();
    private final MutationService mutationService;

    public RegOpsMutationController(MutationService mutationService) {
        this.mutationService = mutationService;
        kiponos.afterValueChanged(change -> {
            log.info("Freeze posture delta: path={} value={}", change.path(), change.newValue());
            if (change.path().startsWith("freeze/")) {
                var notify = kiponos.path("notify");
                slackClient.post(notify.get("slack_freeze_channel"),
                    "Freeze change: " + change.path() + " = " + change.newValue());
            }
            if (kiponos.path("meta").getBool("siem_forward_enabled")) {
                siemClient.emit("regops_freeze_change", change.path(), change.newValue());
            }
        });
    }

    @PostMapping("/admin/mutations")
    public ResponseEntity<?> applyMutation(
            @RequestBody MutationRequest req,
            @RequestHeader(value = "X-Team-Id", required = false) String teamId) {

        if (!isMutationAllowed(teamId, req.scope())) {
            if (kiponos.path("notify").getBool("pager_on_unauthorized_mutation")) {
                pagerClient.trigger("mutation_during_freeze", req);
            }
            return ResponseEntity.status(423).body("change freeze active");
        }
        return ResponseEntity.ok(mutationService.apply(req));
    }

    private boolean isMutationAllowed(String teamId, String scope) {
        if (kiponos.path("freeze").getBool("changes_allowed")) {
            return true;
        }

        var scopes = kiponos.path("scopes");
        if ("admin".equals(scope) && scopes.getBool("block_admin_mutations")) {
            return isBreakGlass(teamId);
        }
        if ("schema".equals(scope) && scopes.getBool("block_schema_migrations")) {
            return isBreakGlass(teamId);
        }
        return isBreakGlass(teamId);
    }

    private boolean isBreakGlass(String teamId) {
        var ex = kiponos.path("exceptions");
        return teamId != null
            && ex.getList("break_glass_team_ids").contains(teamId);
    }
}
```

## Real-world scenarios

| Scenario | Without live freeze tree | With Kiponos RegOps posture |
|----------|-------------------------|----------------------------|
| Audit week Monday 08:00 | Twelve staggered redeploys | `changes_allowed: false` one edit |
| Platform SRE break-glass | Manual env override per pod | `exceptions/break_glass_team_ids` |
| Unauthorized mutation attempt | Silent success on stale JVM | HTTP 423 + pager |
| Audit week ends Saturday | Weekend deploy to unfreeze | Dashboard flip instant |
| Assessor asks freeze evidence | Email + deploy logs | Kiponos ACL + SIEM |

## Performance: freeze checks on mutation path

- **One WebSocket per service JVM** — shared freeze tree
- **Gate is ≤4 local reads** — nanoseconds vs mutation I/O
- **Delta patches** — global freeze in seconds across fleet
- **No `@RefreshScope`** — admin APIs keep connections
- **Scope flags** block only relevant mutation classes

## Compare to alternatives

| Approach | Fleet-wide freeze in seconds | Break-glass exceptions | Scope-specific blocks |
|----------|-------------------------------|------------------------|----------------------|
| Pipeline env freeze | Partial — bypassed by internal APIs | Awkward | No |
| Terraform plan-only | Infra only | No | No |
| Email + honor system | No enforcement | N/A | N/A |
| Per-service env var | Drift across pods | Per-pod chaos | No |
| **Kiponos SDK** | **Yes — one tree** | **Yes** | **Yes** |

## When not to use Kiponos for change freeze

| Boundary | Better home |
|----------|-------------|
| CAB charter and change taxonomy | ITSM / release management wiki |
| Whether freeze satisfies SOC2 CC8 | Auditor + compliance — not this article |
| Kubernetes deployment freeze (replica/image) | GitOps / admission controllers |
| Code merge freezes in GitHub | Branch protection rules |
| Emergency infra failover runbooks | Platform GitOps |

## Getting started (15 minutes)

1. [Create TeamPro at kiponos.io](https://kiponos.io) — profile `['release']['prod']['freeze']`.
2. Add `io.kiponos:sdk-boot-3` to every mutation-gated service.
3. Set `-Dkiponos="['release']['prod']['freeze']"` on all twelve JVMs.
4. Replace `CHANGES_ALLOWED` with `kiponos.path("freeze").getBool("changes_allowed")`.
5. Wire `afterValueChanged` Slack + SIEM notifications.
6. Drill: staging — flip `changes_allowed: false` and confirm all services return 423 **without restart**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
- Related: [GitOps vs live operational config](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-gitops-vs-live-config.md)
- Related: [Config schema versioning](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-config-schema-versioning.md)

---

*Kiponos.io — CAB prose lives in the wiki; changes_allowed lives in the tree.*