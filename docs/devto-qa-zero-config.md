---
title: "QA Environments With Zero Config Files — Kiponos Replaces Them All (Java SDK)"
published: true
tags: java, testing, qa, devops
description: Eliminate application-qa.yml and 40-variable env matrices. QA JVMs read dependencies, stubs, and feature flags from Kiponos — live updates without redeploy.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-qa-zero-config.md
main_image: https://files.catbox.moe/4p1gxw.jpg
---

QA environments are where configuration goes to die. `application-qa.yml`, `.env.qa`, Docker Compose overrides, Helm values for "qa-staging-2", and a wiki page listing **40 environment variables** nobody updates. Add one microservice and the matrix doubles.

[Kiponos.io](https://kiponos.io) collapses QA config into **one live profile** every JVM reads locally: dependency URLs, mock toggles, feature flags, test user seeds, and timeout knobs — editable in the dashboard while exploratory testing is in progress.

## What "zero config files" means

Your QA deployment still needs **auth** to connect:

```bash
export KIPONOS_ID="..."
export KIPONOS_ACCESS="..."
java -Dkiponos="['my-app']['v2']['qa']['integration']" -jar app.jar
```

Everything else — database JDBC URL, payment sandbox endpoint, "use mock fraud service," feature flags — lives in Kiponos, not in Git.

```java
Kiponos kiponos = Kiponos.createForCurrentTeam();

String jdbc = kiponos.path("dependencies", "postgres").get("jdbc_url");
String payments = kiponos.path("dependencies", "payments").get("base_url");
boolean mockFraud = kiponos.path("toggles", "qa").getBool("use_mock_fraud");
```

No `application-qa.yml`. No `SPRING_DATASOURCE_URL` in CI secrets for QA logic — only Kiponos tokens.

## Why QA teams feel this pain

| Static config problem | What breaks |
|----------------------|-------------|
| Wrong YAML branch deployed | Tests pass against prod-like URLs by accident |
| Env var drift between services | Integration tests flake |
| "Restart QA after config change" | Tester loses repro state |
| Per-developer `.env.local` | "Works on my QA" |

QA needs **fast iteration** on dependencies — point at a stub mid-test, crank timeouts for slow partners, enable a feature flag for one scenario — without a redeploy pipeline.

## Architecture

![Architecture diagram](https://files.catbox.moe/piufym.png)

All QA services share one profile slice — **consistent** dependency map across the fleet.

## Example QA tree

```yaml
dependencies/
  postgres/
    jdbc_url: jdbc:postgresql://qa-db:5432/app
  payments/
    base_url: https://sandbox.payments.test
  fraud/
    base_url: http://fraud-mock:8080
toggles/
  qa/
    use_mock_fraud: true
    enable_new_checkout: true
    fail_payment_on_purpose: false
timeouts/
  http_client_ms: 5000
  integration_wait_sec: 30
testdata/
  default_customer_id: cust-qa-001
```

## Exploratory testing: change config mid-session

Tester discovers payment mock is wrong:

1. Open Kiponos dashboard → `dependencies/payments/base_url` → point to new stub
2. WebSocket delta hits all QA JVMs
3. **Next HTTP call** uses new URL — no pod restart, no `kubectl rollout`

For chaos scenarios:

```java
if (kiponos.path("toggles", "qa").getBool("fail_payment_on_purpose")) {
    return PaymentResult.declined("qa_injected_failure");
}
```

Flip `fail_payment_on_purpose` in UI during a manual test — instant repro.

## CI vs manual QA

| Layer | Kiponos role |
|-------|----------------|
| CI pipeline | Inject only `KIPONOS_ID` / `KIPONOS_ACCESS`; profile holds URLs |
| Manual QA | Same profile — engineers tweak live |
| Staging promotion | Different profile path (`['my-app']['staging']`) — same binary |

One artifact, multiple profiles — the [staging mirror pattern](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-staging-live-profile.md) uses the same idea.

## Performance

QA is not prod QPS — but tests still hammer hot paths. `get()` remains **local** — no config server poll per assertion.

## Compare to alternatives

| Approach | Live mid-test changes | Cross-service consistency |
|----------|----------------------|---------------------------|
| application-qa.yml in Git | PR + redeploy | Drift per repo |
| .env in CI | Re-run pipeline | Per-job matrix |
| Shared test DB config table | Possible | DB coupling |
| **Kiponos QA profile** | **Dashboard** | **One tree** |

## Getting started

1. [Free TeamPro at kiponos.io](https://kiponos.io) — create `['my-app']['v2']['qa']['integration']`
2. Copy your current `application-qa.yml` keys into the dashboard tree
3. Remove QA-specific YAML from the repo (keep prod secrets out of Git regardless)
4. Wire SDK reads in `@Configuration` beans
5. Run an integration test; change `payments.base_url` live; re-run without redeploy

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

## What is next

Extend the same model to **CI parallelism tuning** and **automation without env vars** — one hub for the whole delivery lifecycle.

---

*Kiponos.io — real-time config for Java. QA without the YAML graveyard.*