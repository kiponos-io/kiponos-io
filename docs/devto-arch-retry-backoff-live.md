---
title: "Retry and Exponential Backoff Policies That Change While Jobs Run (Kiponos Python SDK)"
published: false
tags: python, architecture, distributed, devops
description: Hard-coded max_retries and backoff caps fail when downstream is flaky. Python workers read live retry policy from Kiponos — tune during incident without killing queues.
canonical_url: https://dev.to/kiponos/retry-and-exponential-backoff-policies-that-change-while-jobs-run-kiponos-python-sdk-3e1i
main_image: https://files.catbox.moe/w2vd1k.jpg
---

Distributed workers embed `max_retries=3` and `backoff=2**attempt`. Partner API degrades; you need **more retries** and **longer backoff** for 20 minutes — then restore. Static constants mean **redeploying consumers** while DLQ fills.

[Kiponos.io](https://kiponos.io) externalizes retry policy:

```python
def retry_policy(kiponos, job_type: str) -> RetryPolicy:
    p = kiponos.path("retry", job_type)
    return RetryPolicy(
        max_attempts=p.get_int("max_attempts"),
        base_sec=p.get_float("base_backoff_sec"),
        max_sec=p.get_float("max_backoff_sec"),
        jitter=p.get_bool("jitter_enabled"),
    )
```

## Retry tree

```yaml
retry/
  webhooks/
    max_attempts: 5
    base_backoff_sec: 2
    max_backoff_sec: 120
    jitter_enabled: true
  etl/
    max_attempts: 2
    base_backoff_sec: 10
  global/
    pause_all_retries: false
```

## Incident: pause retries storm

Set `pause_all_retries: true` — workers read flag before enqueueing retry; storm stops **without** scaling consumers to zero.

## Performance

Read once per job attempt — local `get_int()`.

## Getting started

1. Move retry constants from code to `retry/*`
2. Chaos test: lower `max_attempts`; raise live during simulated outage

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — retry policy is operational data, not a constant in git.*