# Crunchbase News — pending dev.to articles

Batch for **next week**. Profile: https://www.crunchbase.com/organization/kiponos

## On Crunchbase already

| # | id | Title |
|---|-----|-------|
| 1 | 3941363 | Tune Model Training in Real Time |
| 2 | 3941425 | Supervisor Algorithm Retunes Training |

## Pending Crunchbase (32 articles)

| # | Topic | dev.to id | URL |
|---|-------|-----------|-----|
| 3 | Fraud / payment routing | 3941518 | https://dev.to/kiponos/retune-fraud-thresholds-and-payment-routes-in-real-time-no-java-restart-kiponos-sdk-13nf |
| 4 | API rate limits & circuit breakers | 3941575 | https://dev.to/kiponos/change-api-rate-limits-and-circuit-breakers-at-runtime-no-java-redeploy-kiponos-sdk-3d94 |
| 5 | E-commerce A/B checkout | queued | — |
| 6 | Trading bot risk | queued | — |
| 7 | Game server balance | queued | — |
| 8 | IoT sensor calibration | queued | — |
| 9 | Hospital triage | queued | — |
| 10 | LLM inference serving | queued | — |
| 11 | Banking loan approval | queued | — |
| 12 | Banking AML monitoring | queued | — |
| 13 | Accounting month-end | queued | — |
| 14 | Accounting tax rates | queued | — |
| 15 | Insurance underwriting | queued | — |
| 16 | Logistics routing | queued | — |
| 17 | Telecom QoS | queued | — |
| 18 | Energy grid dispatch | queued | — |
| 19 | SaaS multi-tenant | queued | — |
| 20 | CDN edge rules | queued | — |
| 21 | Cybersecurity WAF | queued | — |
| 22 | Real-estate valuation | queued | — |
| 23 | Microservices collaboration | queued | — |
| 24 | Microservices saga | queued | — |
| 25 | Microservices handoff | queued | — |
| 26 | Microservices event routing | queued | — |
| 27 | QA zero config files | queued | — |
| 28 | Automation no env vars | queued | — |
| 29 | CI test tuning | queued | — |
| 30 | Staging live profile | queued | — |
| 31 | K8s no ConfigMaps | queued | — |
| 32 | K8s no restart | queued | — |
| 33 | K8s SDK per pod | queued | — |
| 34 | K8s multi-tenant namespaces | queued | — |

## Refresh after publish series completes

```bash
python3 -c "
import json
m=json.load(open('$HOME/.config/devto/published-manifest.json'))
for path,info in m.items():
    print(info.get('id'), info.get('url',''))
"
```

## Batch workflow (next week)

```bash
python3 ~/.grok/skills/crunchbase-news/scripts/prepare_from_devto.py ARTICLE_ID \
  --json-out /tmp/crunchbase-news.json
# Manual paste into Crunchbase News & Press
```

Full catalog: `docs/devto-article-catalog.md`