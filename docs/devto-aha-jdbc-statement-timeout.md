---
main_image: https://litter.catbox.moe/yca3ar.jpg
title: "spring.datasource.hikari.connection-timeout Was Not Enough — We Changed JDBC statement_timeout Live Mid-Incident (Spring Boot)"
published: false
tags: java, springboot, database, devops
description: Connection pool timeouts protect acquire. Query statement timeouts protect the database. When a bad report query holds backends hostage, Kiponos lets you tighten JDBC statement timeouts without a redeploy.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-jdbc-statement-timeout.md
---

Friday 16:08. Finance re-runs the "quick" month-end export. PostgreSQL `pg_stat_activity` shows **one** `SELECT` chewing 40% of a primary for **eleven minutes**. Cart checkouts start queuing on Hikari. On-call sees pool wait, not the real villain: a statement with **no `statement_timeout`**.

Someone pastes the JDBC URL flag from a wiki:

```
jdbc:postgresql://…?options=-c%20statement_timeout=30s
```

That flag was set at **pod birth**. Changing it means a new deployment. The finance job still runs. Replica lag climbs. The senior DBA says the sentence every Java team has heard:

> "Statement timeout is a **data-layer contract**. We do not flip that in production without a release and a change ticket."

Meanwhile the query plan is fine on 1% of data and catastrophic on end-of-month cardinality. You do not need a new architecture. You need an **operational ceiling** on how long any single statement may own a backend — **now**.

**The Aha:** JDBC / session `statement_timeout` (and siblings like `lock_timeout`, `idle_in_transaction_session_timeout`) are not schema. They are **incident dials**. With [Kiponos.io](https://kiponos.io) you read them on every checkout of a connection (or apply them via a live binder) — zero-latency local gets, dashboard delta in seconds, no redeploy.

## The hard-coded belief

Spring Boot and Hikari make connection **acquire** timeout obvious:

```yaml
spring:
  datasource:
    hikari:
      connection-timeout: 3000
      maximum-pool-size: 20
```

Teams stop there. Acquire timeout means "how long I wait for a free connection." It does **not** mean "how long a stolen connection may run a pathological query." That second dial is usually:

- buried in the JDBC URL once at boot, or
- set in `SET statement_timeout` only in one repository method someone remembered, or
- left to the database default (often **0** — unlimited)

```java
// The trap: pool is "healthy", query is not
try (Connection c = dataSource.getConnection();
     PreparedStatement ps = c.prepareStatement(MONTH_END_SQL)) {
    // no Statement.setQueryTimeout, no session statement_timeout
    try (ResultSet rs = ps.executeQuery()) {
        return map(rs);
    }
}
```

| What teams say | What production does |
|----------------|---------------------|
| "Hikari timeout protects the app" | It protects **waiting for a connection**, not runaway SQL |
| "statement_timeout is a DBA setting" | App sessions override cluster defaults every day |
| "We'll add query timeout in the next epic" | Finance re-runs the export tonight |
| "Global 30s will break batch" | Batch and OLTP need **different** live profiles |

The pain is not missing Redis. It is treating **query ceilings** like compile-time philosophy.

## What is Kiponos.io (for this incident)

[Kiponos.io](https://kiponos.io) is a live operational config hub. Your JVM opens **one WebSocket**, receives a tree snapshot for profile path `['orders']['v1']['prod']['base']`, then **deltas only** when someone edits a key. Hot-path `getInt()` / `getLong()` reads are **local memory** — no HTTP RTT on checkout.

You keep wiring in Git (`DataSource` bean, migrations, schema). You move **operational ceilings** — statement timeout ms, lock timeout ms, whether heavy reports are allowed — into the hub so on-call can tighten the noose without recycling pods.

## Architecture

![Architecture diagram](https://litter.catbox.moe/abjhnb.png)

1. Connect once at startup — `Kiponos.createForCurrentTeam()` or builder with team id + access key.  
2. Snapshot loads; `db_ops/jdbc/*` keys are already in memory.  
3. Dashboard edit of `statement_timeout_ms` → **delta** merge on a worker thread.  
4. Next borrowed connection (or `afterValueChanged` re-apply) runs `SET statement_timeout`.  
5. OLTP requests keep calling `getLong` locally when they need policy for logging / kill switches.

## Config tree

```yaml
db_ops/
  jdbc/
    statement_timeout_ms: 8000
    lock_timeout_ms: 2000
    idle_in_tx_timeout_ms: 15000
    query_timeout_sec: 8          # JDBC Statement.setQueryTimeout
    heavy_report_enabled: false
    heavy_report_timeout_ms: 120000
  profiles/
    oltp:
      statement_timeout_ms: 5000
    batch:
      statement_timeout_ms: 180000
```

Same JAR. Staging profile may use softer ceilings. Prod OLTP stays tight. Month-end **batch worker** uses a different Kiponos profile path — not a secret YAML copy on a laptop.

## Integration — Spring Boot 3

Bootstrap (secrets stay in env / secrets manager; **timeouts do not**):

```java
@Configuration
public class KiponosConfig {

    @Bean(destroyMethod = "disconnect")
    public Kiponos kiponos() {
        return Kiponos.createForCurrentTeam();
        // or builder: teamId, accessKey, profilePath ['orders']['v1']['prod']['base']
    }
}
```

Apply session timeouts whenever a connection is checked out:

```java
@Component
public class LiveJdbcSessionBinder {

    private final Kiponos kiponos;
    private final HikariDataSource dataSource;

    public LiveJdbcSessionBinder(Kiponos kiponos, DataSource dataSource) {
        this.kiponos = kiponos;
        this.dataSource = (HikariDataSource) dataSource;
        this.dataSource.setConnectionInitSql(null); // we set per-checkout for live values
        kiponos.afterValueChanged(ch -> {
            if (ch.path().startsWith("db_ops/jdbc")) {
                // next checkout picks up new ceilings; optional: log for audit
            }
        });
    }

    public Connection borrow() throws SQLException {
        Connection c = dataSource.getConnection();
        var j = kiponos.path("db_ops", "jdbc");
        long stmtMs = j.getLong("statement_timeout_ms", 8_000L);
        long lockMs = j.getLong("lock_timeout_ms", 2_000L);
        long idleMs = j.getLong("idle_in_tx_timeout_ms", 15_000L);
        try (Statement s = c.createStatement()) {
            s.execute("SET statement_timeout = " + stmtMs);
            s.execute("SET lock_timeout = " + lockMs);
            s.execute("SET idle_in_transaction_session_timeout = " + idleMs);
        }
        return c;
    }
}
```

Repository path that also sets JDBC client timeout (defense in depth):

```java
public List<OrderRow> loadRecent(long customerId) throws SQLException {
    var j = kiponos.path("db_ops", "jdbc");
    int jdbcSec = j.getInt("query_timeout_sec", 8);
    try (Connection c = binder.borrow();
         PreparedStatement ps = c.prepareStatement(
             "SELECT id, total FROM orders WHERE customer_id = ? ORDER BY id DESC LIMIT 50")) {
        ps.setQueryTimeout(jdbcSec);
        ps.setLong(1, customerId);
        try (ResultSet rs = ps.executeQuery()) {
            return map(rs);
        }
    }
}
```

Hot-path read is local:

```java
long ceiling = kiponos.path("db_ops", "jdbc").getLong("statement_timeout_ms");
```

No Redis round-trip. No actuator refresh. No `@RefreshScope` bean storm.

## Report job — separate profile, same product

```java
public void runMonthEndExport() {
    var j = kiponos.path("db_ops", "jdbc");
    if (!j.getBoolean("heavy_report_enabled", false)) {
        throw new IllegalStateException("heavy reports disabled live — enable in hub for batch window");
    }
    // batch worker process uses profile ['orders']['v1']['prod']['batch']
    // with heavy_report_timeout_ms: 120000 — not the OLTP 8s ceiling
}
```

Ops enables `heavy_report_enabled` for a 40-minute window, then flips it off. Cart pods never saw the long timeout.

## Belief vs reality

| Belief | Production reality |
|--------|-------------------|
| Pool size is the main DB safety lever | One unlimited query can still torch the primary |
| Timeouts belong only in JDBC URL | URLs freeze at deploy; incidents do not |
| One global timeout fits all | OLTP and batch need **live** different ceilings |
| Kill -9 the pod is fine | You kill healthy traffic sharing the pool |

## Real scenarios

| Moment | Hard-coded reflex | Kiponos path |
|--------|-------------------|--------------|
| Pathological report | Deploy URL with `statement_timeout` | Drop `statement_timeout_ms` to 3000 on OLTP profile |
| Lock pile-up | Restart Postgres | Tighten `lock_timeout_ms` live |
| Idle-in-tx leak | "We'll add a interceptor next sprint" | `idle_in_tx_timeout_ms` now |
| Load test | Special `application-loadtest.yml` | Profile `loadtest/live` in hub |

Pair with [live Hikari pool sizing](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-connection-pool-live.md) and [SQL query limits](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-sql-query-limit.md) — pool, statement ceiling, and row caps are three dials of the same incident.

## Before / after

| Approach | Change statement timeout mid-incident | Cost on hot path |
|----------|--------------------------------------|------------------|
| JDBC URL in deployment | PR + rollout | Frozen until restart |
| DBA `ALTER DATABASE` only | Cluster-wide blast radius | Affects every client |
| Poll config server per query | Dashboard-fast | Network on every SQL |
| **Kiponos + session SET** | **Seconds, per profile** | **Local get + SET on checkout** |

## Performance

- One WebSocket per process; deltas are small  
- `getLong` / `getInt` are O(1) memory reads  
- `SET statement_timeout` on checkout is cheap vs query time  
- Do **not** re-SET on every statement inside a tight loop unless you must — checkout binding is enough for most OLTP

## When not to use Kiponos for this

| Case | Better approach |
|------|-----------------|
| Fixing a missing index | Migration + query rewrite |
| Cluster max_connections | Capacity planning / PgBouncer |
| Row-level security policy | Schema / migration review |
| "Timeout = 1ms to pass the fire drill" | Load testing honesty first |

## Getting started

1. Create a [TeamPro](https://kiponos.io) profile `['orders']['v1']['prod']['base']`.  
2. Move `statement_timeout_ms`, `lock_timeout_ms`, `query_timeout_sec` into `db_ops/jdbc`.  
3. Add `LiveJdbcSessionBinder` (or a Hikari `ConnectionCustomizer` that reads Kiponos).  
4. Game day: run a slow query in staging, drop timeout from 60s → 3s in the dashboard, watch cancel without pod restart.  
5. Document: Git owns schema and wiring; hub owns **how hard the database may be squeezed tonight**.

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io) · [GETTING-STARTED](https://github.com/kiponos-io/kiponos-io/blob/master/GETTING-STARTED.md)

---

*Kiponos.io — connection pools are not enough. Query ceilings are live operational state.*
