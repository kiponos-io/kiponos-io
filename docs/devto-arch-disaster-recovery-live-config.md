---
title: "Disaster Recovery Runbooks as Live Config — Flip read_only, Drain, and Fallback Endpoints in One Tree (Java SDK)"
published: false
tags: java, resilience, sre, architecture
description: DR runbooks trapped in Confluence mean rolling restarts during outages. Kiponos holds mode=read_only, drain flags, and fallback endpoints in one live tree — SRE flips knobs while JVMs keep running.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-disaster-recovery-live-config.md
main_image: https://files.catbox.moe/id94bo.jpg
---

Thursday 03:41 UTC. **us-east-1** primary Postgres stops accepting writes — storage subsystem fault, ETA unknown. The on-call SRE opens the disaster recovery runbook: step 4 says *"set `READ_ONLY_MODE=true` in `values-prod.yaml`, commit, and rolling restart checkout pods."* Step 7 says *"point `PAYMENT_API_URL` at the DR endpoint in us-west-2."* Step 9 says *"enable `DRAIN_NEW_ORDERS` on the ingress layer."*

Twelve minutes in, checkout pods are still accepting writes against a database that returns `SQLSTATE 40001`. Traffic has not drained because the ingress team is waiting for the Helm PR to merge. Fallback processor URLs are still compiled into a `@Value` constant nobody has touched since the last game day — in 2023.

The incident commander asks the question every postmortem eventually surfaces:

> "Why is our **runbook** a sequence of deploy steps when the failure mode needs **seconds**, not a Git pipeline?"

Most Java payment and order services encode DR posture as **three different artifacts**: a wiki checklist, environment variables for endpoint URLs, and a `static final boolean READ_ONLY` that only changes after a restart. [Kiponos.io](https://kiponos.io) collapses that into **one operational tree** — `mode=read_only`, drain flags, fallback endpoints — readable on every request path with **local `get*()` calls** and flippable from the dashboard while processes run.

## The problem: runbook steps baked into immutable config

A typical order service gates writes like this on every checkout:

```java
@PostMapping("/orders")
public OrderResponse createOrder(@RequestBody OrderRequest req) {
    if (!primaryDbHealth.isWritable()) {
        throw new ServiceUnavailableException("primary degraded");
    }
    return orderService.place(req);
}
```

DR posture usually lives elsewhere — scattered and static:

```yaml
# application-prod.yml — requires rolling restart to change
app:
  mode: read_write
  payment-api-url: https://payments-primary.internal
  drain-new-orders: false
```

Or worse — hard-coded in Java because "DR never happens":

```java
private static final String PAYMENT_API = "https://payments-primary.internal";
private static final boolean DRAIN_NEW_ORDERS = false;

public HttpClient paymentClient() {
    return HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();  // base URL chosen once at @PostConstruct from PAYMENT_API
}
```

The hot path executes **thousands of times per second**. During an outage you need to:

1. Flip **`mode`** from `read_write` → `read_only` before corrupting data
2. Set **`drain.accept_new_traffic`** so ingress and app agree on rejection semantics
3. Swap **`endpoints.payment_api`** to the warm standby region

Doing that through Helm while the primary is on fire is not resilience — it is **documentation theater**.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "DR runbooks are fine — we rehearse them quarterly" | Rehearsal uses staging keys; prod constants diverged six months ago |
| "Read-only mode belongs in feature flags" | Flag SaaS latency and boolean model ignore `drain_grace_seconds` and endpoint URLs |
| "We'll flip DNS to the DR region" | App-layer URLs are still compiled to primary; DNS change alone does not reroute HTTP clients |
| "ConfigMap sync is fast enough" | Kubelet relist + rolling restart = minutes; writes keep landing on a dead primary |
| "Separate DR config repo keeps blast radius small" | Three repos mean three merge queues during the incident |

## The architecture insight

**Disaster recovery posture is operational config, not bootstrap archaeology.** The same knobs your runbook tells humans to edit — service mode, drain switches, fallback endpoints — belong in **one live tree** the JVM already reads on the request path. Kiponos makes runbook step 4, 7, and 9 a **dashboard edit**, not a deploy pipeline.

## What Kiponos.io is for DR runbooks

[Kiponos.io](https://kiponos.io) is a real-time configuration hub. Each Spring Boot service connects **once** at startup over WebSocket; the profile tree — for example `['checkout']['v3']['prod']['live']` — loads into an **in-memory cache** inside the Java SDK.

When SRE sets `mode: read_only` and `endpoints.payment_api` to the us-west standby, a **delta** patches only those keys. The next `kiponos.path("ops", "mode").get()` on an incoming POST is a **local memory read** — no HTTP to a config API, no JDBC poll, no Redis round-trip on the checkout path.

`afterValueChanged` listeners let you log audit trails, increment metrics, and warm HTTP connection pools to the new fallback endpoint **without** restarting the JVM.

No restart. No redeploy. No `@RefreshScope` bean recycle.

## Reference architecture

![Architecture diagram](https://files.catbox.moe/5212yb.png)

**Runbook documents the tree; the tree drives behavior.** Keep prose in Confluence for context and escalation — but the **authoritative values** live in Kiponos where flipping them takes seconds.

## Config tree — mode, drain, and fallback in one place

Five folders — `ops`, `drain`, `endpoints`, `health`, `audit`:

```yaml
ops/
  mode: read_write              # read_only | read_write | maintenance
  maintenance_message: "Checkout paused — DR failover in progress"
  fail_open_on_hub_partition: false
drain/
  accept_new_traffic: true
  reject_new_writes: false
  grace_seconds: 120
  return_retry_after_sec: 30
endpoints/
  payment_api: https://payments-use1.internal
  payment_api_fallback: https://payments-usw2.internal
  ledger_api: https://ledger-use1.internal
  ledger_api_fallback: https://ledger-usw2.internal
  active_payment_route: primary    # primary | fallback
health/
  primary_db_writable: true        # fed by health checker or external signal
  min_healthy_replicas_pct: 50
audit/
  last_dr_flip_by: ""
  last_dr_flip_at_ms: 0
```

One tree. One profile path: `['checkout']['v3']['prod']['live']`. Game day and production share **identical key layout** — only values differ.

## Java integration: hot-path gate + live endpoint routing

```java
import io.kiponos.sdk.Kiponos;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

@Configuration
public class DrLiveConfigBeans {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();

    @Bean
    Kiponos kiponosClient() {
        kiponos.afterValueChanged(change ->
            log.info("DR config delta: path={} value={}", change.path(), change.newValue())
        );
        return kiponos;
    }

    @Bean
    Filter drModeGate(Kiponos kiponos) {
        return new DrReadOnlyGate(kiponos);
    }
}

public class DrReadOnlyGate implements Filter {
    private final Kiponos kiponos;

    public DrReadOnlyGate(Kiponos kiponos) {
        this.kiponos = kiponos;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        var ops = kiponos.path("ops");
        var drain = kiponos.path("drain");
        HttpServletRequest http = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String mode = ops.get("mode");
        boolean acceptTraffic = drain.getBool("accept_new_traffic");
        boolean rejectWrites = drain.getBool("reject_new_writes");

        if ("maintenance".equals(mode) || !acceptTraffic) {
            response.setStatus(503);
            response.setHeader("Retry-After",
                String.valueOf(drain.getInt("return_retry_after_sec")));
            response.getWriter().write(ops.get("maintenance_message"));
            return;
        }

        if ("read_only".equals(mode) && rejectWrites && isMutating(http)) {
            response.setStatus(503);
            response.setHeader("Retry-After",
                String.valueOf(drain.getInt("return_retry_after_sec")));
            response.getWriter().write("Service in read-only mode — writes temporarily disabled");
            return;
        }

        chain.doFilter(req, res);
    }

    private boolean isMutating(HttpServletRequest req) {
        String method = req.getMethod();
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)
            || "DELETE".equals(method);
    }
}

@Service
public class PaymentRoutingClient {
    private final Kiponos kiponos;
    private final AtomicReference<RestClient> clientRef = new AtomicReference<>();

    public PaymentRoutingClient(Kiponos kiponos) {
        this.kiponos = kiponos;
        kiponos.afterValueChanged(change -> {
            if (change.path().startsWith("endpoints/")) {
                clientRef.set(buildClient(resolvePaymentBaseUrl()));
            }
        });
        clientRef.set(buildClient(resolvePaymentBaseUrl()));
    }

    public PaymentResult authorize(PaymentRequest req) {
        return clientRef.get()
            .post()
            .uri("/v1/authorize")
            .body(req)
            .retrieve()
            .body(PaymentResult.class);
    }

    private String resolvePaymentBaseUrl() {
        var ep = kiponos.path("endpoints");
        String route = ep.get("active_payment_route");
        return "fallback".equals(route)
            ? ep.get("payment_api_fallback")
            : ep.get("payment_api");
    }

    private RestClient buildClient(String baseUrl) {
        return RestClient.builder()
            .baseUrl(baseUrl)
            .build();
    }
}
```

Every `get()`, `getBool()`, and `getInt()` on the request path is **O(1) local cache** — microseconds, not cross-region config service RTT.

Wire `afterValueChanged` on `active_payment_route` so the HTTP client pool rebuilds toward us-west-2 **before** the next authorization — no pod bounce required.

## Real-world scenarios

| Scenario | Without live DR tree | With Kiponos one-tree runbook |
|----------|----------------------|-------------------------------|
| Primary DB write failure | Hunt three repos; rolling restart while writes corrupt | Flip `ops/mode: read_only` + `drain/reject_new_writes: true` in dashboard |
| Regional payment API outage | Rebuild image with new `@Value` URL | Set `endpoints/active_payment_route: fallback` live |
| Controlled failover rehearsal | Staging uses different key names than prod | Same tree shape; rehearsal flips real keys in `staging` profile |
| Hub partition during DR | Unknown — pods may serve stale `read_write` | `fail_open_on_hub_partition: false` → fail closed; metrics on connection state |
| Recovery — restore primary | Second deploy wave to undo DR env vars | Single edit: `mode: read_write`, `active_payment_route: primary` |

## Performance: why DR gates must not add network I/O

- **One WebSocket per JVM** — not one config fetch per checkout request
- **Mode and drain checks are three local string/bool reads** — nanoseconds vs authorization I/O
- **Delta patches** — flipping `mode` sends one patch, not a full tree reload to every pod
- **Endpoint swap via listener** — `RestClient` rebuild happens once per key change, not per transaction
- **No GC pressure** from re-parsing YAML on every POST during traffic spikes

In load tests, Kiponos reads are noise on the order path; card-network and ledger RTT dominate latency.

## Compare to alternatives

| Approach | Mid-incident DR flip | Hot-path read latency | Single tree for mode + drain + endpoints |
|----------|---------------------|----------------------|------------------------------------------|
| Confluence + Helm PR | No — pipeline bound | Zero (static) but stale | No — scattered artifacts |
| Kubernetes ConfigMap | Minutes — relist + restart | Zero if baked at startup | Partial — no live flip without restart |
| Redis config hash | Yes with poll | Poll interval adds tail latency | Possible — custom schema discipline |
| Feature-flag SaaS | Booleans only | SDK network on evaluation | No — URLs and grace seconds awkward |
| `@RefreshScope` Spring | Bean recycle — drops in-flight | Zero after refresh storm | Partial — still restart-like blast |
| **Kiponos SDK** | **Yes — dashboard delta** | **Zero (in-process cache)** | **Yes — one profile tree** |

## When not to use Kiponos for disaster recovery

| Boundary | Better home |
|----------|-------------|
| DNS failover, BGP announcements, cross-region VPC routing | Cloud provider / network runbook — not app config |
| Database promotion, replica lag cutover, binlog coordinates | DBA tooling with explicit consistency guarantees |
| Secrets for DR credentials (standby API keys, break-glass tokens) | Vault / sealed-secrets — not live dashboard |
| Immutable infrastructure identity (cluster name, service account ARNs) | GitOps → cluster reconcile |
| Regulatory requirement for two-person approval on DR activation | Workflow tool gates the **dashboard ACL**, not the SDK pattern |

## Getting started (15 minutes)

1. [Create TeamPro at kiponos.io](https://kiponos.io) — add profile `['checkout']['v3']['prod']['live']` with `ops`, `drain`, and `endpoints` folders matching the tree above.
2. Add `io.kiponos:sdk-boot-3` to your Spring Boot order service.
3. Set `KIPONOS_ID`, `KIPONOS_ACCESS`, and `-Dkiponos="['checkout']['v3']['prod']['live']"`.
4. Replace `READ_ONLY_MODE` env vars and `@Value` payment URLs with `kiponos.path("ops", ...)` and `kiponos.path("endpoints", ...)`.
5. Register `DrReadOnlyGate` as a servlet `Filter` and `PaymentRoutingClient` with `afterValueChanged` pool rebuild.
6. Game day: in staging, flip `mode: read_only` then `active_payment_route: fallback` — confirm 503 semantics and us-west traffic **without pod restart**. Document the key names in your Confluence runbook.

## Further reading

- [Developer Quickstart](https://dev.to/kiponos/kiponosio-developer-quickstart-java-python-and-your-first-live-config-change-3kjo)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
- Related: [Graceful shutdown drain policy](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-graceful-shutdown.md)
- Related: [Multi-region active-active bounds](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-multi-region-active-active.md)

---

*Kiponos.io — your DR runbook is a checklist; the tree is what actually saves the database.*