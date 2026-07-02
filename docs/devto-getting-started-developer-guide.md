---
title: "Kiponos.io Developer Quickstart — Java, Python, and Your First Live Config Change"
published: false
tags: java, python, tutorial, devops
description: Step-by-step for developers new to Kiponos — signup, profile paths, SDK connect, local zero-latency reads, delta WebSocket updates, and your first live change without restart.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md
main_image: https://files.catbox.moe/4p1gxw.jpg
---

You heard that [Kiponos.io](https://kiponos.io) changes application behavior **while the process keeps running** — no redeploy, no restart, no refresh. This guide is for **developers integrating the SDK for the first time**. Product tour and screenshots: [Getting Started with Kiponos.io (dev.to)](https://dev.to/kiponos/getting-started-with-kiponosio-p5k). Clone-and-run golden example: [GETTING-STARTED.md on GitHub](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md).

## What problem Kiponos solves

Most teams treat these as **permanent**:

- `application.yml` floats (pool sizes, timeouts, thresholds)
- `private static final int MAX_RETRIES = 3`
- `@Retryable(maxAttempts = 3)`
- Resilience4j YAML in Git

Changing them means PR → CI → deploy → restart. Kiponos separates:

| Class | Where it lives |
|-------|----------------|
| **Bootstrap wiring** (URLs, bean graph, secrets refs) | Git / Spring config |
| **Operational knobs** (limits, thresholds, intervals) | Kiponos hub + SDK local reads |

Your app **connects once**, holds the tree **in memory**, and reads with **zero network** on the hot path. Ops edits the dashboard → **WebSocket delta** patches one key → next `get()` sees it.

## Architecture (one picture)

![Architecture diagram](https://files.catbox.moe/1e6x7l.png)

## Step 1 — Sign up and create a profile path

1. [kiponos.io/signup](https://kiponos.io/signup) — TeamPro / team starter (free).
2. Onboarding wizard: **Application** → **Release** → **Environment** → **Config name**.
3. Example profile bracket path (used everywhere below):

```
['my-app']['v1.0.0']['dev']['live']
```

4. In the dashboard, create folders and keys — like a filesystem:

```yaml
demo/
  limits/
    max_requests_per_sec: 100
    enabled: true
  messages/
    greeting: "Hello"
```

**Also read:** [Getting Started with Kiponos.io (dev.to)](https://dev.to/kiponos/getting-started-with-kiponosio-p5k) for dashboard screenshots and folder UI.

## Step 2 — Get SDK credentials

Dashboard: **Sidebar → Config SDK → Generate keys** for your environment.

| Credential | Purpose |
|------------|---------|
| `KIPONOS_ID` | Identity token (JWE) |
| `KIPONOS_ACCESS` | Access token (JWE) |

Reuse the **same pair** for every app on that environment; disambiguate with profile path (`-Dkiponos` or `KIPONOS_PROFILE`).

Never commit real tokens — use env vars or CI secrets in production.

## Step 3 — Java / Spring Boot integration

### Dependency

```groovy
// build.gradle
implementation 'io.kiponos:sdk-boot-3:4.4.0.250319'
```

[Maven Central](https://central.sonatype.com/artifact/io.kiponos/sdk-boot-3)

### Environment

```bash
export KIPONOS_ID="your_id_token"
export KIPONOS_ACCESS="your_access_token"
```

Run with profile:

```bash
java -Dkiponos="['my-app']['v1.0.0']['dev']['live']" -jar my-service.jar
```

### Spring Boot 3 bean

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

### Read on the hot path (zero latency)

```java
@Service
public class DemoService {
    private final Kiponos kiponos;

    public DemoService(Kiponos kiponos) {
        this.kiponos = kiponos;
    }

    public String greet() {
        return kiponos.path("demo", "messages").getString("greeting", "Hello");
    }

    public boolean allowRequest() {
        var limits = kiponos.path("demo", "limits");
        if (!limits.getBool("enabled", true)) return true;
        int max = limits.getInt("max_requests_per_sec", 100);
        return rateCheck(max); // your logic; max is local read
    }
}
```

### React to changes (optional)

```java
kiponos.afterValueChanged(change ->
    log.info("[kiponos] {} = {}", change.getPath(), change.getNewValue()));
```

Use for pool resize, Logback level, WebClient rebuild — **not** on every request.

### Golden example (clone and run)

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/golden/java
# Edit build.gradle placeholders, then:
./gradlew run
```

Full walkthrough: [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)

## Step 4 — Python integration

```python
import os
from kiponos import Kiponos

os.environ["KIPONOS_ID"] = "your_id_token"
os.environ["KIPONOS_ACCESS"] = "your_access_token"
os.environ["KIPONOS_PROFILE"] = "['my-app']['v1.0.0']['dev']['live']"

kiponos = Kiponos.create_for_current_team()

def current_limit() -> int:
    return kiponos.path("demo", "limits").get_int("max_requests_per_sec", 100)
```

Safe inside `async for`, training loops, webhook retry — `get_int()` is local.

```python
kiponos.after_value_changed(
    lambda c: print(f"[kiponos] {c.path} = {c.new_value}")
)
```

## Step 5 — Your first live change (the Aha)

1. Start your app with SDK connected (log should show handshake / configs ready).
2. Call a code path that reads `demo/messages/greeting` (or your key).
3. Open the Kiponos dashboard — change `greeting` to a new string.
4. **Without restarting**, call the code path again.

The new value appears immediately. That is the core loop every use-case article builds on.

## How it works under the hood

| Phase | Behavior |
|-------|----------|
| Connect | WebSocket to `wss://kiponos.io/api/io-kiponos-sdk` |
| Snapshot | Full tree for profile loads into SDK memory |
| Steady state | `get*()` = in-process lookup |
| Edit | Dashboard sends **delta only** for changed key |
| Merge | Background thread patches tree |
| Next read | Same OS process, fresher value |

No polling. No per-request HTTP to Kiponos on the read path.

## Performance expectations

- One WebSocket per process
- Reads are O(1) on cached nodes
- Deltas are small — editing one `int` does not retransmit the whole tree
- Hot path remains bound by **your** I/O and CPU, not config

## Common mistakes

| Symptom | Fix |
|---------|-----|
| Auth / handshake error | Regenerate tokens; no trailing whitespace |
| Empty or wrong values | Profile path must match dashboard exactly |
| Change not visible | Confirm key path; call `get` on same profile |
| `@RefreshScope` habits | Operational keys do not need Spring refresh |

## When not to use Kiponos

| Use | Better tool |
|-----|-------------|
| Replica count, Ingress, RBAC | GitOps |
| Secrets / API keys | Vault, sealed-secrets |
| Application code structure | Git |
| Boolean experiments with audience rules | Feature-flag product (or Kiponos for ops floats) |

## What to read next

Use-case deep dives (hard-coded pain → live change):

- [Mind reader demo + ICU live config](https://dev.to/kiponos/the-mind-reader-demo-trick-same-zero-latency-config-that-changes-icu-parameters-live-spring-boot-17oj)
- [Change API rate limits at runtime](https://dev.to/kiponos/change-api-rate-limits-and-circuit-breakers-at-runtime-no-java-redeploy-kiponos-sdk-3d94)
- [Tune model training in real time (Python)](https://dev.to/kiponos/tune-model-training-in-real-time-zero-latency-zero-restarts-kiponos-python-sdk-510j)

## Reference links

- [kiponos.io](https://kiponos.io) — signup and dashboard
- [Getting Started with Kiponos.io (dev.to)](https://dev.to/kiponos/getting-started-with-kiponosio-p5k) — product tour
- [GETTING-STARTED.md (GitHub)](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md) — golden Java example, troubleshooting
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io) — SDK, skills, examples

---

*Kiponos.io — connect once, read locally, change behavior from the dashboard while your app runs.*