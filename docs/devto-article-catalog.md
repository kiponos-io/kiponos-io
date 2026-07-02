# Kiponos.io dev.to Article Catalog

Master list of use-case articles. Publish queue: `~/.config/devto/publish-queue.txt`

**Quality bar:** Gold-standard articles (~150+ lines) include problem framing, architecture diagram, config tree, code integration, scenarios table, performance, alternatives, getting started. Wave 2–5 and Wave 6 were rewritten to match Wave 1 (ML/fraud) depth.

## Wave 1 — Runtime tuning (reference quality)

| # | Topic | SDK | File | Status |
|---|-------|-----|------|--------|
| 1 | Live ML hyperparameter tuning | Python | `devto-realtime-ml-training.md` | published |
| 2 | Supervisor orchestrating training | Python | `devto-supervisor-ml-training.md` | published |
| 3 | Fraud / payment routing | Java | `devto-fraud-payment-routing.md` | published |
| 4 | API rate limits & circuit breakers | Java | `devto-rate-limits-circuit-breakers.md` | published |
| 5 | E-commerce A/B checkout weights | Java | `devto-ab-checkout-weights.md` | published |
| 6 | Trading bot risk caps | Python | `devto-trading-bot-risk.md` | published |
| 7 | Game server balance patches | Java | `devto-game-server-balance.md` | published |
| 8 | IoT sensor calibration | Python | `devto-iot-sensor-calibration.md` | published |
| 9 | Hospital triage routing | Java | `devto-hospital-triage-routing.md` | published |
| 10 | LLM inference serving | Python | `devto-llm-inference-serving.md` | published |

## Wave 2 — Industries (rewritten)

| # | Topic | SDK | File | Status |
|---|-------|-----|------|--------|
| 11 | Retail banking loan approval thresholds | Java | `devto-banking-loan-approval.md` | published |
| 12 | AML transaction monitoring rules | Java | `devto-banking-aml-monitoring.md` | published |
| 13 | Accounting month-end close controls | Java | `devto-accounting-month-end.md` | published |
| 14 | Multi-jurisdiction tax rate tables | Java | `devto-accounting-tax-rates.md` | published |
| 15 | Insurance underwriting score cutoffs | Java | `devto-insurance-underwriting.md` | published |
| 16 | Logistics fleet route parameters | Python | `devto-logistics-routing.md` | published |
| 17 | Telecom QoS and bandwidth routing | Java | `devto-telecom-qos.md` | published |
| 18 | Energy grid load dispatch limits | Python | `devto-energy-grid.md` | published |
| 19 | SaaS multi-tenant feature entitlements | Java | `devto-saas-multitenant.md` | published |
| 20 | CDN edge cache and routing rules | Java | `devto-cdn-edge-rules.md` | published |
| 21 | Live WAF and security rule tuning | Java | `devto-cybersecurity-waf.md` | published |
| 22 | Real-estate valuation model weights | Python | `devto-realestate-valuation.md` | published |

## Wave 3 — Microservices (rewritten)

| # | Topic | SDK | File | Status |
|---|-------|-----|------|--------|
| 23 | Shared collaboration config tree | Java | `devto-microservices-collaboration.md` | published |
| 24 | Saga compensation timeouts | Java | `devto-microservices-saga.md` | queued |
| 25 | Cross-service handoff signals | Python | `devto-microservices-handoff.md` | queued |
| 26 | Event bus topic routing live | Java | `devto-microservices-event-routing.md` | queued |

## Wave 4 — QA & lifecycle (rewritten)

| # | Topic | SDK | File | Status |
|---|-------|-----|------|--------|
| 27 | QA environments with zero config files | Java | `devto-qa-zero-config.md` | queued |
| 28 | Automation testing without env variables | Python | `devto-automation-no-env.md` | queued |
| 29 | CI test parallelism and timeout tuning | Python | `devto-ci-test-tuning.md` | queued |
| 30 | Staging mirrors prod — live profile overrides | Java | `devto-staging-live-profile.md` | queued |

## Wave 5 — Kubernetes (rewritten)

| # | Topic | SDK | File | Status |
|---|-------|-----|------|--------|
| 31 | Pods without ConfigMaps or env vars | Java | `devto-k8s-no-configmaps.md` | queued |
| 32 | Config without pod restart | Java | `devto-k8s-no-restart.md` | queued |
| 33 | One SDK per pod — local reads at scale | Java | `devto-k8s-sdk-per-pod.md` | queued |
| 34 | Multi-tenant namespaces + live hub | Java | `devto-k8s-multitenant.md` | queued |

## Wave 6 — Architecture & platform pain points (new)

Not industry-specific. Design patterns, lifecycle chaos, and technical architecture where **live config** is the innovation.

| # | Topic | SDK | File | Status |
|---|-------|-----|------|--------|
| 35 | Multi-environment configuration chaos | Java | `devto-arch-config-chaos-multi-env.md` | queued |
| 36 | Feature flags vs live config hub | Architecture | `devto-arch-feature-flags-vs-config-hub.md` | queued |
| 37 | Circuit breaker & bulkhead thresholds live | Java | `devto-arch-circuit-breaker-bulkhead.md` | queued |
| 38 | Canary traffic weights without a mesh | Java | `devto-arch-canary-traffic-weights.md` | queued |
| 39 | Retry & exponential backoff policies live | Python | `devto-arch-retry-backoff-live.md` | queued |
| 40 | Strangler fig migration routing | Java | `devto-arch-strangler-fig-migration.md` | queued |
| 41 | JDBC/HTTP connection pool tuning live | Java | `devto-arch-connection-pool-live.md` | queued |
| 42 | Observability alert thresholds live | Python | `devto-arch-observability-thresholds.md` | queued |

## Wave 11 — Aha moments (hard-coded → live)

Emotional incident story → Aha (change live) → WebSocket/delta/local reads. Published 2026-07-02 batch.

| # | Topic | File | dev.to ID | Status |
|---|-------|------|-----------|--------|
| 51 | Tomcat maxThreads | `devto-aha-tomcat-threads.md` | #4045437 | published |
| 52 | Retry maxAttempts | `devto-aha-retry-attempts.md` | #4045448 | published |
| 53 | Log levels | `devto-aha-log-levels.md` | #4045453 | published |
| 54 | Batch chunk size | `devto-aha-batch-chunk.md` | #4045462 | published |
| 55 | Rate limiter permits | `devto-aha-rate-limiter.md` | #4045468 | published |
| 56 | Cache TTL | `devto-aha-cache-ttl.md` | #4045470 | published |
| 57 | HTTP read timeout | `devto-aha-http-timeout.md` | #4045473 | published |
| 58 | Webhook backoff | `devto-aha-webhook-retry.md` | #4045478 | published |

Also published: Mind reader + ICU blended (`devto-mind-reader-live-ops.md` #4045312).

## Wave 12–14 — Aha moments (published 2026-07-02)

24 articles — see `wave12/13/14-aha-publish-queue.txt`. Scripts: `publish-all-aha-waves.sh`, `setup-aha-covers.sh`.

## Developer Quickstart (new)

| Topic | File | dev.to ID | Status |
|-------|------|-----------|--------|
| Java + Python quickstart | `devto-getting-started-developer-guide.md` | #4047302 | published |

Links: [Developer Quickstart (dev.to)](https://dev.to/kiponos/kiponosio-developer-quickstart-java-python-and-your-first-live-config-change-3kjo) · [Product tour (dev.to)](https://dev.to/kiponos/getting-started-with-kiponosio-p5k) · [GETTING-STARTED.md (GitHub)](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)

Wave 12–14 articles expanded with newcomer primer + inline getting started (2026-07-02).

**Total: 76+ articles** | Gap between publishes: 2–3 hours (random)

**Scripts:**
- Publish queue: `scripts/publish-devto-queue.sh`
- Push fixes to live posts: `scripts/update-published-devto.sh`