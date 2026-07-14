# Example 05 — Spring Boot 3 REST: `@Value` frozen until restart

| | |
|--|--|
| **Level** | Core |
| **App shape** | **Spring Boot 3** REST API |
| **Industry** | Any microservice owner / platform team |
| **Pain** | “`@Value` is frozen until we restart the pod” |
| **SDK** | `createForCurrentTeam` as a `@Bean`, path/get on request, `@PreDestroy` `disconnect` |

## Business problem

Your timeout, greeting string, or rate limit lives in `application.yml` and is injected once with `@Value` or `@ConfigurationProperties`.

Ops needs a different number **now** — not after a PR, CI, image bake, and rolling restart. `@RefreshScope` is a partial answer that still churns beans and misses half the graph.

That is **configuration hell** for Spring services: human judgment blocked by process lifecycle.

## What this example does

A minimal Spring Boot 3 app exposes:

| Endpoint | Behavior |
|----------|----------|
| `GET /api/ops` | Live `request-timeout-ms` + `greeting` from Kiponos |
| `GET /api/hello` | Hot-path style read of the same keys |

Tree under your connected profile:

```text
examples / spring-boot-rest / request-timeout-ms
examples / spring-boot-rest / greeting
```

Ops flips a key in the **Kiponos.io dashboard**. Hit the endpoint again — **no redeploy**, no context refresh ceremony. The Kiponos bean disconnects cleanly via `@PreDestroy`.

## Why Kiponos fits

| Old world | Kiponos |
|-----------|---------|
| `@Value` frozen until restart | `get` / `getInt` from hub cache on demand |
| `@RefreshScope` bean recycle | Live tree; optional hooks for pools later |
| Secrets + knobs mixed in yml | Bootstrap in Spring; **ops knobs** in the hub |
| “Who changed prod timeout?” | Dashboard history (later: `dumpConfig`) |

Real-time hub = the on-call brain is connected to the running Spring process.

## Prerequisites

1. Free [TeamPro](https://kiponos.io) account  
2. From **Connect**: `KIPONOS_ID`, `KIPONOS_ACCESS`, config profile path  
3. Java 17+

## Run

```bash
cd examples/java/05-spring-boot3-rest-api

export KIPONOS_ID='…from Connect…'
export KIPONOS_ACCESS='…from Connect…'
# optional; default in build.gradle is my-app/v1.0.0/dev/base
export KIPONOS="['my-app']['v1.0.0']['dev']['base']"

./gradlew bootRun
```

Then:

```bash
curl -s http://localhost:8080/api/ops | jq
curl -s http://localhost:8080/api/hello | jq
```

### Expected output (first run)

Creates `examples/spring-boot-rest` keys if missing (`request-timeout-ms=3000`, default greeting), then JSON shows live values.

### Golden E2E + logic tests

```bash
./gradlew test
```

- `LiveOpsLogicTest` — pure parsers (always runs)  
- `SpringRestGoldenTest` — live handshake + ensure keys; **skips** if tokens are still `REPLACE_WITH_*`

## Dashboard exercise

1. Open Kiponos.io → your env  
2. Find `examples → spring-boot-rest → greeting`  
3. Change the string (e.g. `Timeout tuned live`)  
4. Optionally set `request-timeout-ms` to `1500`  
5. `curl` `/api/ops` again — **no restart**  

## Files

| Path | Role |
|------|------|
| `RestApiApp.java` | Spring Boot entry |
| `KiponosConfig.java` | `@Bean` + `@PreDestroy` disconnect |
| `LiveOpsConfig.java` | Ensure folders; live reads |
| `OpsController.java` | REST `/api/ops`, `/api/hello` |
| `LiveOpsLogicTest.java` | Unit logic |
| `SpringRestGoldenTest.java` | Live SDK golden |
| `build.gradle` | Spring Boot 3 + SDK + env wiring |

## Next

See the full catalog: [`examples/CATALOG.md`](../../CATALOG.md)
