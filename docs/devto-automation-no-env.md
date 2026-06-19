---
title: "Automation Testing Without Environment Variables (Kiponos Python SDK)"
published: true
tags: python, testing, automation, qa
description: Selenium, Playwright, and pytest suites read targets and credentials paths from Kiponos — no .env, no CI secret matrix for test config.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-automation-no-env.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-automation-no-env.jpg
---

CI pipelines drown in env vars: `BASE_URL`, `API_KEY_PATH`, `TIMEOUT`, `BROWSER`, per-suite overrides. Rotating a staging URL means editing GitHub Actions secrets and re-running everything.

**Kiponos replaces the env-var layer for test configuration:**

```python
kiponos = Kiponos.create_for_current_team()
BASE = kiponos.path("automation", "targets").get("web_app_url")
TIMEOUT = kiponos.path("automation", "selenium").get_int("implicit_wait_sec")
```

QA changes `web_app_url` in Kiponos when staging moves — **the next test run** picks it up. No pipeline edit. No `.env` file in the repo.

Works for Playwright, pytest, Robot Framework — any Python runner that can import the SDK once at session start.

Pair with [zero-config QA Java services](https://dev.to/kiponos) — tests and SUT read the same live profile.

[kiponos.io](https://kiponos.io) · [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)