# Kiponos Public Examples Catalog

**Repo:** [kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)  
**Goal:** One runnable example per common SDK surface, app shape, industry pain, and experience level — so almost any developer can find “their” story.

**Rules (all examples):**
- Never commit real `KIPONOS_ID` / `KIPONOS_ACCESS` (placeholders + env override for local golden runs)
- Always `disconnect()` on shutdown
- Each example: `README.md` (problem → Kiponos fix → run) + tests
- Java first (model); Python parity rows marked **Python later**

---

## Legend

| Column | Meaning |
|--------|---------|
| **ID** | Stable slug under `examples/{java\|python}/` |
| **Level** | `intro` · `core` · `advanced` · `edge` |
| **App shape** | How the process is packaged |
| **Pain** | Config-file hell complaint this attacks |
| **SDK API** | Surfaces demonstrated |

---

## A. Application shapes (Java)

| ID | Level | App shape | Pain | SDK API |
|----|-------|-----------|------|---------|
| `01-standalone-3am-kill-switch` | intro | **Standalone `main`** | Redeploy to flip an emergency switch at 3am | create, path, getBoolean, set, disconnect |
| `02-standalone-multi-env-profile` | intro | Standalone `main` | Same jar, wrong env file copied to prod | `-Dkiponos` profiles, get |
| `03-library-embedded-defaults` | core | **Java library** used by hosts | Library ships `.properties` nobody can change | path/get from library code |
| `04-library-spi-plugin-knobs` | advanced | Library + SPI plugins | Plugin configs scattered across 12 yml files | nested folders per plugin |
| `05-spring-boot3-rest-api` | core | **Spring Boot 3** REST | `@Value` frozen until restart | bean + `@PreDestroy` disconnect |
| `06-spring-boot2-legacy` | core | **Spring Boot 2** | Legacy stack stuck on javax | sdk-boot-2 surface |
| `07-spring-boot-actuator-live` | advanced | Spring Boot + Actuator | Ops wants knobs without new endpoints | live get in health/info |
| `08-spring-batch-job-params` | core | Spring Batch | Job params baked into XML | getInt/get for chunk size |
| `09-quarkus-cli-style` | core | Quarkus (or plain Graal-ready main) | Native image rebuild for one flag | path/get at runtime |
| `10-micronaut-service` | core | Micronaut | Config maps ≠ live ops | createForCurrentTeam |
| `11-android-desktop-swing` | intro | **Swing / desktop** (see also `examples/comm-panel`) | UI strings and layout in code | live title/size/position |
| `12-javafx-kiosk` | core | JavaFX kiosk | Field techs can’t ship new APK/jar | live media URLs |
| `13-servlet-war-tomcat` | advanced | WAR on Tomcat | Ops restarts whole node for one property | SDK in ServletContextListener |
| `14-dropwizard-service` | core | Dropwizard | YAML hell across clusters | path/get |
| `15-vertx-verticle` | advanced | Vert.x | Redeploy verticle for timeout | live timeouts |
| `16-kafka-consumer-worker` | core | Kafka consumer process | Lag vs poison-message tradeoff hard-coded | live prefetch / pause flag |
| `17-scheduled-cron-worker` | intro | Cron / systemd timer job | Schedule in crontab only | live cron expression + enabled |
| `18-cli-tool-flags` | intro | CLI (`picocli`/args4j) | Flags wrong in prod scripts | Kiponos overrides CLI defaults |
| `19-fat-jar-shipping` | core | Fat jar distribution | Customers edit unpackaged conf wrong | remote hub instead of local conf |
| `20-modular-jpms` | edge | JPMS modules | Modulepath + conf fragmentation | SDK from app module |

---

## B. SDK API surface matrix (every important case)

| ID | Level | Scenario | Pain | SDK API |
|----|-------|----------|------|---------|
| `api-01-path-get-typed` | intro | Nested folders + typed get | Stringly typed yml everywhere | path, get, getInt, getLong, getBoolean |
| `api-02-defaults-and-missing` | core | Missing keys | NPE vs silent wrong default | get(key, def), getOrEmpty, getOrNull |
| `api-03-create-folder-set` | core | Bootstrap tree from code | Onboarding needs empty dashboard | folderOrCreate, set, createFolder |
| `api-04-hooks-value-updated` | core | React to dashboard edits | Polling files / SIGHUP | afterValueUpdated |
| `api-05-hooks-key-lifecycle` | advanced | Key create/rename/delete | Cache invalidation wrong | afterKeyCreated/Renamed/Deleted |
| `api-06-hooks-folder-created` | advanced | Dynamic tenants/folders | Multi-tenant folder spawn | afterFolderCreated |
| `api-07-hooks-item-saved` | advanced | Full item save events | Partial update races | afterItemSaved |
| `api-08-disconnect-lifecycle` | intro | Clean shutdown | Leaked WS / zombie reconnect | disconnect + shutdown hook |
| `api-09-dump-config` | core | Incident forensics | “What was config at 02:14?” | dumpConfig |
| `api-10-offline-lkg-reads` | advanced | Network blip | App dies when config server dies | Offline + LKG reads |
| `api-11-safe-mode-fail-closed` | edge | Auth/config catastrophe | Wrong defaults take payments | Safe mode empty/null |
| `api-12-mode-transparency` | core | Ops dashboards | “Are we live?” unknown | getCurrentMode, isReady/Offline/Safe |
| `api-13-multi-profile-dev-stage-prod` | core | Env promotion | Copy-paste conf between envs | three `-Dkiponos` profiles |
| `api-14-self-log-toggle` | edge | SDK log noise | Log flood in prod | self folder log flags |
| `api-15-rename-delete-folder` | advanced | Tree hygiene | Stale folders forever | renameFolder, deleteFolder |
| `api-16-incr-counter` | advanced | Distributed-ish counter | Local files not shared | incr |
| `api-17-concurrent-readers` | edge | Many threads read config | Stale caches / locks | multi-thread get |
| `api-18-long-running-live-loop` | intro | Process stays up | Only restart sees changes | loop + hooks |

---

## C. Classic config-hell pain themes (cross-cutting)

| Theme | What people yell about | Example IDs |
|-------|------------------------|-------------|
| **Redeploy to change a string** | “Just change the URL” | 01, 05, api-18 |
| **Env file roulette** | Wrong `.env` on the box | 02, api-13 |
| **Secrets mixed with knobs** | Vault for DB password; yml for timeout | docs pattern + 05 |
| **GitOps lag** | PR + CI for a timeout | 01, 16, finops-* |
| **Snowflake servers** | Hand-edited conf on host 7 | 19 |
| **Christmas tree YAML** | 3k-line application.yml | 04, api-01 |
| **Feature flags vs config** | LD for flags, files for ops knobs | 01, retail-* |
| **Multi-tenant overrides** | Per-customer CSV hell | hospitality, saas-multi-tenant |
| **Regional differences** | EU vs US timeouts | edgeops, travel |
| **Nightly batch params** | Ops edits XML at midnight | 08, 17 |
| **Incident throttle** | Can’t slow traffic without deploy | reliability-* |
| **Compliance sampling** | Audit log volume vs risk | regops-* |

---

## D. Industries & business verticals (config hell → Kiponos)

Each row is a **story-shaped** example (Java primary; Python parity later).

### FinTech / payments
| ID | Level | Pain | Kiponos angle |
|----|-------|------|---------------|
| `fintech-processor-kill-switch` | intro | Stop a flaky acquirer without deploy | live boolean |
| `fintech-velocity-limits` | core | Fraud velocity limits need minute-level changes | nested limits tree |
| `fintech-fx-spread-bps` | advanced | Treasury changes spreads intraday | getInt live |
| `fintech-settlement-batch-size` | core | Night batch OOMs | live chunk size |

### E‑commerce / retail
| ID | Level | Pain | Kiponos angle |
|----|-------|------|---------------|
| `retail-dynamic-pricing-caps` | core | Cap discounts during flash sales | live max % |
| `retail-inventory-reorder` | core | Reorder points lag spreadsheet | live thresholds |
| `retail-checkout-timeout` | intro | PSP timeout wrong in Black Friday | live ms |

### Travel / hospitality
| ID | Level | Pain | Kiponos angle |
|----|-------|------|---------------|
| `travel-overbooking-limits` | core | Overbooking % by property | folder per property |
| `travel-rate-shop-throttle` | advanced | Channel manager API rate limits | live RPS |

### Media / streaming
| ID | Level | Pain | Kiponos angle |
|----|-------|------|---------------|
| `media-bitrate-ladder` | core | Ladder wrong for a region | live ladder JSON-ish keys |
| `media-cdn-failover` | advanced | Origin failover without DNS panic | live origin URL |

### Health / pharma
| ID | Level | Pain | Kiponos angle |
|----|-------|------|---------------|
| `health-appointment-slot-rules` | core | Slot length rules change by clinic | path per clinic |
| `pharma-trial-enrollment-caps` | advanced | Cap enrollment by site overnight | live caps |

### Public sector
| ID | Level | Pain | Kiponos angle |
|----|-------|------|---------------|
| `public-permit-sla-hours` | core | SLA hours change after ordinance | live SLA |
| `public-form-feature-flags` | intro | Forms go live without release train | kill switches |

### Logistics / construction
| ID | Level | Pain | Kiponos angle |
|----|-------|------|---------------|
| `logistics-dispatch-radius` | core | Dispatch radius by depot | live km |
| `construction-weather-cutoff` | intro | Weather cutoff for pours | live boolean + hours |

### EdTech
| ID | Level | Pain | Kiponos angle |
|----|-------|------|---------------|
| `edtech-exam-proctoring` | advanced | Proctoring sensitivity mid-exam season | live thresholds |

### Sports / gaming
| ID | Level | Pain | Kiponos angle |
|----|-------|------|---------------|
| `sports-odds-caps` | advanced | Risk caps during big match | live max stake |
| `gaming-event-loot-weights` | core | Liveops weights without client patch | live weights |

### AI / ML ops
| ID | Level | Pain | Kiponos angle |
|----|-------|------|---------------|
| `ai-prompt-guardrails-live` | core | Prompt policy without redeploy models | live strings |
| `ai-inference-batch-size` | core | GPU batch size vs latency | getInt live |
| `ai-drift-threshold` | advanced | Drift alert threshold tuning | live double-as-string |

### Data / streaming ops
| ID | Level | Pain | Kiponos angle |
|----|-------|------|---------------|
| `dataops-kafka-lag-threshold` | core | Consumer lag alarm noise | live lag max |
| `dataops-spark-shuffle-parts` | advanced | Shuffle partitions experiment | live int |

### Platform / SRE
| ID | Level | Pain | Kiponos angle |
|----|-------|------|---------------|
| `sre-retry-budget` | core | Retry storms | live budget |
| `sre-degradation-mode` | intro | “Read-only mode” button | kill switch tree |
| `sre-sample-rate-traces` | core | Trace sampling costs | live rate |

### Edge / CDN / multi-region
| ID | Level | Pain | Kiponos angle |
|----|-------|------|---------------|
| `edge-traffic-weights` | advanced | Steering weights | live weights |
| `edge-waf-sensitivity` | advanced | WAF mode per region | nested region folders |

---

## E. Python parity track (model after Java)

| ID | Mirrors Java | Notes |
|----|--------------|-------|
| `py-01-standalone-kill-switch` | 01 | `kiponos-pysdk` when ready |
| `py-02-fastapi-live-timeout` | 05 | FastAPI lifespan disconnect |
| `py-03-celery-worker-knobs` | 16/17 | task rates live |
| `py-04-django-feature-flag` | 01 | settings.py anti-pattern fixed |
| `py-05-offline-lkg` | api-10 | parity after Java LKG solid |

**Status:** catalog only until Python SDK parity sprint.

---

## F. Experience levels — “find yourself”

| You are… | Start here |
|----------|------------|
| First day with Kiponos | `golden/java`, then `01-standalone-3am-kill-switch` |
| Spring Boot microservice owner | `05-spring-boot3-rest-api` |
| On-call SRE | `sre-degradation-mode`, `api-10-offline-lkg-reads` |
| Library author | `03-library-embedded-defaults` |
| FinTech backend | `fintech-processor-kill-switch` |
| Data platform | `dataops-kafka-lag-threshold` |
| Liveops / product | `gaming-event-loot-weights`, `retail-dynamic-pricing-caps` |
| Compliance | `api-09-dump-config`, regops themes |
| Multi-env DevOps | `02-standalone-multi-env-profile`, `api-13` |

---


## G. Super Patterns (Gang of Four + Kiponos)

Each classic pattern becomes a **Super Pattern**: same structure, selection/policy live in the hub so humans *and* remote SDKs can change behavior without redeploy.

| ID | Pattern | Super Pattern | Status |
|----|---------|---------------|--------|
| `pattern-strategy-live-router` | Strategy | Live Strategy Router | Java + Python |
| `pattern-decorator-live-chain` | Decorator | Live Decorator Chain | Java + Python |
| `pattern-chain-live-fraud` | Chain of Responsibility | Live Handler Chain | Java + Python |
| `pattern-state-live-order` | State | Live State Machine | Java + Python |
| `pattern-factory-live-channel` | Factory Method | Live Product Factory | Java + Python |
| `pattern-adapter-live-psp` | Adapter | Live PSP Adapter | Java |
| `pattern-proxy-live-access` | Proxy | Live Access Proxy | Java |
| `pattern-abstract-factory-live-region` | Abstract Factory | Live Region Family | Java |
| `pattern-bridge-live-implementor` | Bridge | Live Implementor | Java |
| `pattern-facade-live-knobs` | Facade | Live Facade Knobs | Java |
| `pattern-observer-live-bus` … `pattern-visitor-live-registry` | Wave C–D | see Super Patterns backlog | planned |

Full backlog: `kiponos-io-ops/docs-queues/super-patterns-gof-backlog.md` · idea: `~/.grok/ideas/kiponos-gof-super-patterns/`

---

## H. Example folder contract

```text
examples/java/<id>/
  README.md           # human: problem, business story, Kiponos fix, run steps
  MEDIUM.md           # optional story seed for Medium (fun voice)
  build.gradle        # placeholders + env override for CI/local golden
  settings.gradle
  gradlew*
  src/main/java/...   # demo app
  src/test/java/...   # *GoldenTest — real SDK E2E when tokens present
```

**Pipeline fields** (`examples/pipeline/QUEUE.md`): id · status · github_path · medium_url · crunchbase · published_at

---

## I. Publish pipeline (per example)

1. Implement example + golden E2E  
2. Run golden locally with env tokens (never commit tokens)  
3. Push to `kiponos-io/kiponos-io` (`git@gh-kiponos-io:...`)  
4. Publish Medium story (human voice, moral arc, not dry API docs)  
5. Update Crunchbase press reference  
6. Organic pause (~8100s class, same as dev.to waves) before next example  

---

## J. Queue order (first wave)

1. **`01-standalone-3am-kill-switch`** ← current  
2. `02-standalone-multi-env-profile`  
3. `05-spring-boot3-rest-api`  
4. `api-04-hooks-value-updated`  
5. `sre-degradation-mode`  
6. `fintech-processor-kill-switch` (deep industry variant of 01)  
7. `api-10-offline-lkg-reads`  
8. `retail-checkout-timeout`  
9. `16-kafka-consumer-worker`  
10. `03-library-embedded-defaults`  

Full backlog = sections A–E above (~80+ intended modules over time).
