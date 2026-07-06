---
title: "Automation Testing Without Environment Variables (Kiponos Python SDK)"
published: true
tags: python, testing, automation, devops
description: Replace brittle env-var matrices in test runners with a live Kiponos profile. Python pytest suites read URLs, credentials scopes, and chaos toggles locally — update mid-run from the dashboard.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-automation-no-env.md
main_image: https://files.catbox.moe/srkslu.jpg
---

Automation engineers love pytest. They hate maintaining `export API_URL=...` in six CI jobs, `.env.test`, `tox.ini`, GitHub Actions secrets, and a Confluence table that is wrong by Tuesday.

[Kiponos.io](https://kiponos.io) moves **test runtime config** into a live profile: endpoints, feature toggles, parallelism, retry policy, and injected failure switches — read from Python with **zero latency**, updated from the dashboard without re-running the pipeline.

## The env-var matrix anti-pattern

```python
BASE_URL = os.environ["AUTOMATION_API_URL"]
TIMEOUT = int(os.environ.get("HTTP_TIMEOUT", "30"))
USE_MOCK = os.environ.get("USE_MOCK_PAYMENTS", "false") == "true"
```

Every new scenario adds variables. CI caches stale values. Local runs diverge from Jenkins. Rotating a sandbox URL means editing four repos.

## Kiponos model: tokens in env, everything else in the tree

```python
import os
from kiponos import Kiponos

os.environ["KIPONOS_ID"] = os.environ["KIPONOS_ID"]      # only secret in CI
os.environ["KIPONOS_ACCESS"] = os.environ["KIPONOS_ACCESS"]
os.environ["KIPONOS_PROFILE"] = "['automation']['v1']['ci']['e2e']"

kiponos = Kiponos.create_for_current_team()

def client_config():
    dep = kiponos.path("targets", "api")
    return {
        "base_url": dep.get("base_url"),
        "timeout": dep.get_int("timeout_sec"),
        "use_mock_payments": kiponos.path("toggles").get_bool("mock_payments"),
    }
```

CI job exports **two tokens**. The profile holds the behavior.

## Automation config tree

```yaml
targets/
  api/
    base_url: https://api.qa.example.com
    timeout_sec: 30
  browser/
    headless: true
    slow_mo_ms: 0
toggles/
  mock_payments: true
  skip_flaky_suite: false
  inject_500_on_health: false
retries/
  max_attempts: 3
  backoff_sec: 2
suites/
  smoke/
    enabled: true
  regression/
    enabled: true
    shard_count: 4
```

## Mid-run changes (local or long CI)

Running a **two-hour regression** on a shared QA stack? Partner changes sandbox URL:

1. QA lead updates `targets/api/base_url` in Kiponos
2. Connected test workers pick up delta via WebSocket
3. **Next test case** hits the new URL — no pipeline restart

For chaos:

```python
@pytest.fixture
def api_session():
    cfg = client_config()
    if kiponos.path("toggles").get_bool("inject_500_on_health"):
        pytest.skip("health chaos enabled in Kiponos")
    return Session(base_url=cfg["base_url"], timeout=cfg["timeout"])
```

## pytest integration pattern

```python
# conftest.py — connect once per session
@pytest.fixture(scope="session")
def kiponos_client():
    return Kiponos.create_for_current_team()

def pytest_collection_modifyitems(config, items):
    k = Kiponos.create_for_current_team()
    if k.path("toggles").get_bool("skip_flaky_suite"):
        skip = pytest.mark.skip(reason="flaky suite disabled in Kiponos")
        for item in items:
            if "flaky" in item.keywords:
                item.add_marker(skip)
```

Suite enablement becomes **ops-controllable** — disable flaky shard during incident without editing `pytest.ini`.

## Real-world scenarios

| Scenario | Live Kiponos change |
|----------|---------------------|
| Sandbox URL rotation | Edit `targets/api/base_url` |
| Reduce load on QA | Lower `suites/regression/shard_count` |
| Stop flaky failures blocking release | `skip_flaky_suite: true` |
| Chaos drill | `inject_500_on_health: true` |

## Performance

Test loops may call config often — `get_bool()` stays **local**. No HTTP to a "test config service" per assertion.

## Compare to alternatives

| Approach | Change without new CI run | Single source for all suites |
|----------|---------------------------|------------------------------|
| Env vars only | No | Fragmented |
| config.json in repo | PR + merge | Git lag |
| Consul/etcd for tests | Ops-heavy | Possible |
| **Kiponos profile** | **Dashboard** | **One tree** |

## Getting started

1. [Free TeamPro at kiponos.io](https://kiponos.io) — profile `['automation']['v1']['ci']['e2e']`
2. Migrate env vars into dashboard folders (`targets`, `toggles`, `retries`)
3. CI: pass only `KIPONOS_ID` and `KIPONOS_ACCESS`
4. Run pytest; flip `mock_payments` in UI; confirm next test uses mock

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

## What is next

Pair with **CI parallelism tuning** and **QA zero-config Java services** — full pipeline lives in Kiponos profiles, not scattered env files.

---

*Kiponos.io — real-time config for Python. Automation without the env-var spreadsheet.*