# Crunchbase News — pending dev.to articles

Articles published on dev.to that still need a **News & Press** entry on
https://www.crunchbase.com/organization/kiponos

**Already on Crunchbase:** #1 (3941363), #2 (3941425)

| # | Topic | dev.to id | URL | Crunchbase |
|---|-------|-----------|-----|------------|
| 3 | Fraud / payment routing (Java) | 3941518 | https://dev.to/kiponos/retune-fraud-thresholds-and-payment-routes-in-real-time-no-java-restart-kiponos-sdk-13nf | pending |
| 4 | API rate limits & circuit breakers | 3941575 | https://dev.to/kiponos/change-api-rate-limits-and-circuit-breakers-at-runtime-no-java-redeploy-kiponos-sdk-3d94 | pending |
| 5 | E-commerce A/B checkout weights | scheduling | — | pending |
| 6 | Trading bot risk caps | scheduling | — | pending |
| 7 | Game server balance patches | scheduling | — | pending |
| 8 | IoT sensor calibration | scheduling | — | pending |
| 9 | Hospital triage routing | scheduling | — | pending |
| 10 | LLM inference serving | scheduling | — | pending |

## Staggered publish (articles #5–#10)

Running in background: `scripts/publish-devto-series.sh`

- Random gap between posts: **2–3 hours**
- Log: `~/.config/devto/series-publish.log`
- Article #4 published 2026-06-19 ~18:17; #5 expected ~20:25

Update ids/urls in this file after the series completes (`grep '"id"' ~/.config/devto/series-publish.log`).

## Batch workflow (next week)

```bash
python3 ~/.grok/skills/crunchbase-news/scripts/prepare_from_devto.py ARTICLE_ID \
  --json-out /tmp/crunchbase-news.json
# Manual paste into Crunchbase, or Playwright --headed on desktop
```