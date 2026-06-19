---
title: "QA Environments With Zero Config Files — Kiponos Replaces Them All (Java SDK)"
published: true
tags: java, testing, qa, devops
description: Eliminate application.yml, .env, and per-QA config files. Java QA stacks read everything from Kiponos SDK — live updates, no env var matrix.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-qa-zero-config.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-qa-zero-config.jpg
---

QA environments are config file graveyards: `application-qa.yml`, `.env.qa`, Docker compose overrides, and "just export these 40 variables." Every new microservice multiplies the pain.

**Kiponos eliminates static QA config entirely.**

```java
// Boot: only auth tokens + profile — no YAML URLs, no env var URLs
Kiponos kiponos = Kiponos.createForCurrentTeam();
String dbUrl = kiponos.path("dependencies", "postgres").get("jdbc_url");
String mockPayments = kiponos.path("dependencies", "payments").get("base_url");
```

The QA profile (`['my-app']['v1']['qa']['base']`) holds **every** dependency endpoint, feature flag, and test double URL. QA engineers change values in the dashboard; **all connected QA JVMs** update live.

**No config files in the repo for QA.** No `.env` in CI. No "wrong QA file" deploys. One profile per environment slice.

Automation testers point manual exploratory QA at the same live tree — flip a dependency to a stub mid-session without redeploy.

[kiponos.io](https://kiponos.io) · [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)