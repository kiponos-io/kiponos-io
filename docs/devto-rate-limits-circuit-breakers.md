---
title: "Change API Rate Limits and Circuit Breakers at Runtime — No Java Redeploy (Kiponos SDK)"
published: true
tags: java, devops, api, realtime
description: Tune per-tenant rate limits and circuit breaker thresholds while your API gateway keeps serving traffic. Kiponos Java SDK delivers local reads with zero latency.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-rate-limits-circuit-breakers.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-rate-limits.jpg
---

An incident at 3 AM is the wrong time to discover your rate limits are in `application.yml`. You need to **throttle a noisy tenant**, **open a circuit** on a failing downstream, or **raise limits** for a launch partner — without rolling pods.

[Kiponos.io](https://kiponos.io) gives Java API services **live** rate-limit and circuit-breaker config: WebSocket deltas into an in-memory SDK cache, local reads on every request.

## The hot-path constraint

```java
if (breaker.isOpen("payments-svc")) {
    return Response.status(503).entity("degraded").build();
}
if (rateLimiter.exceeded(tenantId)) {
    return Response.status(429).entity("slow down").build();
}
```

These checks run on **every request**. Polling Redis or a config service per call adds milliseconds and failure modes. Static YAML means redeploy during an outage.

Kiponos pattern: connect once, read `kiponos.path("limits", tenantId).getInt("rpm")` from local memory.

## Config tree example

```yaml
limits/
  default/
    rpm: 1200
    burst: 200
  tenant_acme/
    rpm: 5000
    burst: 800
breakers/
  payments-svc/
    failure_threshold: 0.5
    open_seconds: 30
    half_open_requests: 5
  inventory-svc/
    failure_threshold: 0.3
    open_seconds: 60
```

## Spring Boot filter integration

```java
@Service
public class LiveGatewayPolicy {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();

    public boolean allow(String tenantId) {
        var node = kiponos.path("limits", tenantIdOrDefault(tenantId));
        int rpm = node.getInt("rpm");
        return localRpmCounter(tenantId).tryAcquire(rpm);
    }

    public int breakerOpenSeconds(String downstream) {
        return kiponos.path("breakers", downstream).getInt("open_seconds");
    }
}
```

Ops changes `tenant_acme.rpm` in the dashboard → next request sees it. No restart.

## Incident scenarios

| Event | Action in Kiponos UI |
|-------|----------------------|
| Tenant abuse | Lower `rpm` for that tenant |
| Downstream melting | Raise `failure_threshold` or increase `open_seconds` |
| Product launch | Bump partner tenant limits live |
| Recovery | Close breaker config / restore defaults |

## Performance

- **Local `getInt()`** — microseconds per request
- **Delta updates** — one changed limit does not reload entire tree
- **One WebSocket** per JVM — not one config fetch per HTTP call

Golden example + Agent Skills: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for Java. Tune gates and breakers while traffic flows.*