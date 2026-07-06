---
title: "spring.data.redis.lettuce.pool.max-active=8 Was Cache Orthodoxy — We Raised It Live During the Session Stampede (Lettuce)"
published: false
tags: java, redis, springboot, performance
description: Lettuce pool max-active feels like Redis client wiring set once in YAML. When session lookups spike and pool wait times explode, max-active is operational — Kiponos resizes the pool live without JVM restart.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-redis-pool-max.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-aha-redis-pool-max.jpg
---

Flash sale minute 22. Redis latency is fine — **12ms p99** — but your storefront pods report `RedisConnectionException: Pool exhausted` because every instance caps Lettuce at **8 active connections** (`spring.data.redis.lettuce.pool.max-active=8`), a number copied from a "safe defaults" internal wiki page when Redis was still a cache sidecar.

Session lookups spike 40×. Each HTTP request borrows a connection. Threads block on `pool.borrow()` while Redis sits mostly idle — not CPU-bound, **client-pool-bound**.

The platform lead says the line everyone has heard:

> "Redis pool size is **client infrastructure**. We do not change that without capacity review."

So you open a PR. CI queues. Someone asks if 16 or 32 will "overload Redis." Checkout keeps timing out while you debate a knob that was never architecture — it was **operational concurrency to the cache tier**.

Here is the moment that clicks for most staff engineers:

**`max-active` behaves like a sacred constant, but it is a dial you need when traffic shape moves faster than your YAML.**

You can turn that dial **while the JVM keeps serving traffic** — no redeploy, no restart, no `@RefreshScope` refresh. Your app runs. You change the number remotely. The binder patches the live `GenericObjectPool` backing Lettuce.

That is [Kiponos.io](https://kiponos.io).

## Step 1 — The hard-coded belief

Spring Boot makes it easy to bake Lettuce pool settings into YAML:

```yaml
spring:
  data:
    redis:
      host: redis-session.internal
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
          max-wait: 2000ms
```

Teams treat this like Redis client schema. It is not. It is **runtime concurrency to the session store** — like Hikari `maximumPoolSize`, like Tomcat `maxThreads`, like a bulkhead. It should move when borrow wait times move.

Your session filter hits the pool on every authenticated request:

```java
@Component
public class SessionHydrationFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redis;

    @Override
    protected void doFilterInternal(
            HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String sessionId = req.getHeader("X-Session-Id");
        String json = redis.opsForValue().get("sess:" + sessionId);
        req.setAttribute("session", parse(json));
        chain.doFilter(req, res);
    }
}
```

When `max-active=8` meets Black Friday concurrency, threads block on pool borrow — not Redis RTT. Problems:

1. **False Redis outages** — app logs blame Redis; Redis metrics look healthy
2. **Deploy to loosen** — rolling restart during peak amplifies session churn
3. **One size for all phases** — calm week and flash sale share the same cap

| What teams say | What production does |
|----------------|---------------------|
| "Eight connections protects Redis" | Redis often has headroom; the **client pool** is the bottleneck |
| "Pool size needs infra sign-off" | Borrow-wait spikes do not wait for committees |
| "Scale pods instead of pool size" | More pods × 8 connections can **worse** Redis connection count |
| "Lettuce pool is bootstrap YAML" | Pool max is operational load policy |

## Step 2 — The Aha: change behavior while the app runs

Wire Lettuce operational knobs into Kiponos:

```yaml
redis/
  lettuce/
    session/
      max_active: 8
      max_idle: 8
      min_idle: 2
      max_wait_ms: 2000
    rate_limit/
      max_active: 4
      max_idle: 4
  ops/
    stampede_mode: false
    stampede_max_active: 24
    log_pool_stats_on_change: true
```

During the stampede, ops enables `stampede_mode` and sets `stampede_max_active: 24`. WebSocket delivers a **delta**. Your binder applies it to the live pool:

```java
@Component
public class LiveLettucePoolBinder {

    private final Kiponos kiponos;
    private final LettuceConnectionFactory connectionFactory;

    public LiveLettucePoolBinder(Kiponos kiponos, LettuceConnectionFactory connectionFactory) {
        this.kiponos = kiponos;
        this.connectionFactory = connectionFactory;
        kiponos.afterValueChanged(this::onChange);
        applyNow();
    }

    private void onChange(ValueChange change) {
        if (change.path().startsWith("redis/lettuce")
                || change.path().startsWith("redis/ops")) {
            applyNow();
        }
    }

    private void applyNow() {
        int maxActive = resolveMaxActive();
        var poolConfig = connectionFactory.getClientConfiguration()
                .getPoolConfig()
                .orElseThrow();
        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMaxIdle(
                kiponos.path("redis", "lettuce", "session").getInt("max_idle", maxActive));
        if (kiponos.path("redis", "ops").getBool("log_pool_stats_on_change", true)) {
            log.warn("Lettuce max_active → {}", maxActive);
        }
    }

    private int resolveMaxActive() {
        if (kiponos.path("redis", "ops").getBool("stampede_mode", false)) {
            return kiponos.path("redis", "ops").getInt("stampede_max_active", 24);
        }
        return kiponos.path("redis", "lettuce", "session").getInt("max_active", 8);
    }
}
```

**No restart.** The pool accepts more concurrent borrows while requests flow. Hot-path reads for guards stay local:

```java
int maxActive = kiponos.path("redis", "lettuce", "session").getInt("max_active");
```

## Step 3 — Architecture

![Architecture diagram](https://files.catbox.moe/93ggtw.png)

1. **Connect once** at startup.
2. **Full tree snapshot** for profile `['storefront']['prod']['redis']`.
3. **Dashboard edit** sends delta only.
4. **SDK merges async** on WebSocket worker thread.
5. **Reads are local** — filter never blocks on config network.

## Bootstrap Kiponos in Spring Boot 3

```java
@Configuration
public class KiponosConfig {

    @Bean
    public Kiponos kiponos(
            @Value("${kiponos.team-id}") String teamId,
            @Value("${kiponos.access-key}") String accessKey,
            @Value("${kiponos.profile-path}") String profilePath) {
        return Kiponos.builder()
                .teamId(teamId)
                .accessKey(accessKey)
                .profilePath(profilePath)
                .build();
    }
}
```

Keep Redis **host and password** in Git/Vault — not operational pool floats.

## Real scenarios

| Moment | Hard-coded reflex | Kiponos path |
|--------|-------------------|--------------|
| Flash sale session spike | PR to raise `max-active` | `redis/ops/stampede_mode` live |
| Redis CPU actually high | Lower pool + shed load | `stampede_max_active` down + rate limit |
| Load test week | Branch per environment | Hub profile `loadtest/aggressive` |
| Post-sale calm | Forgot to revert YAML | Disable stampede mode from dashboard |

Pair with [live Tomcat tuning](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-tomcat-threads.md) — HTTP threads and Redis borrows often exhaust together.

## Before / after

| Approach | Raise max-active during stampede | Hot-path read cost |
|----------|----------------------------------|-------------------|
| `application-prod.yml` | PR + deploy | N/A until restart |
| `@RefreshScope` Redis factory | Context refresh | Connection churn |
| Poll Redis for pool config | Possible | RTT per request |
| **Kiponos SDK** | **Dashboard (seconds)** | **Memory read** |

## Performance — why there is no hit

- `setMaxTotal` on change — not per `GET`
- `getInt()` is O(1) on cached tree
- One WebSocket per JVM lifetime
- Delta updates — stampede toggle patches two keys
- Larger pool removes borrow blocking — often **cuts** p99 more than read cost

## When not to use Kiponos for Lettuce pool

| Case | Better approach |
|------|-----------------|
| Redis cluster topology and shard count | Infrastructure GitOps |
| TLS certificates and AUTH password | Vault + Git |
| Switching Lettuce → Redisson architecture | Code migration |
| `max-active: 500` without Redis memory math | Capacity testing first |

## Getting started

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['storefront']['prod']['redis']`.
2. Move `max_active`, `max_idle`, `max_wait_ms` into hub tree.
3. Add `LiveLettucePoolBinder` with `afterValueChanged`.
4. Game day: spike session traffic in staging, enable `stampede_mode`, watch pool waits vanish **without pod restart**.
5. Document boundary: Git declares Redis endpoint; hub declares **client concurrency**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — Lettuce max-active is a live borrow dial, not cache orthodoxy from the wiki.*