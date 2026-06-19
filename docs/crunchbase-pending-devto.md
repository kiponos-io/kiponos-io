# Crunchbase News — pending dev.to articles

Articles published on dev.to that still need a **News & Press** entry on
https://www.crunchbase.com/organization/kiponos

**Already on Crunchbase:** #1, #2

| # | Topic | dev.to id | URL | Crunchbase |
|---|-------|-----------|-----|------------|
| 3 | Fraud / payment routing (Java) | 3941518 | https://dev.to/kiponos/retune-fraud-thresholds-and-payment-routes-in-real-time-no-java-restart-kiponos-sdk-13nf | pending |
| 4 | API rate limits & circuit breakers | TBD | TBD | pending |
| 5 | E-commerce A/B checkout weights | TBD | TBD | pending |
| 6 | Trading bot risk caps | TBD | TBD | pending |
| 7 | Game server balance patches | TBD | TBD | pending |
| 8 | IoT sensor calibration | TBD | TBD | pending |
| 9 | Hospital triage routing | TBD | TBD | pending |
| 10 | LLM inference serving | TBD | TBD | pending |

## Batch workflow (next week)

For each row:

```bash
python3 ~/.grok/skills/crunchbase-news/scripts/prepare_from_devto.py ARTICLE_ID \
  --json-out /tmp/crunchbase-news.json

# Manual: paste brief into Crunchbase UI
# Or Playwright on desktop:
~/.grok/skills/crunchbase-news/.venv/bin/python \
  ~/.grok/skills/crunchbase-news/scripts/add_news_playwright.py \
  /tmp/crunchbase-news.json --headed
```

Update this file with article ids/urls as the staggered publish series completes.
Check progress: `tail -f ~/.config/devto/series-publish.log`