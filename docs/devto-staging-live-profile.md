---
title: "Staging Mirrors Prod With Live Profile Overrides — No Config Drift (Kiponos Java SDK)"
published: true
tags: java, staging, devops, realtime
description: One Kiponos profile shape for staging and prod; ops toggles overrides live. Eliminates staging YAML forks and env-var drift.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-staging-live-profile.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-staging-profile.jpg
---

Staging diverges from prod because teams maintain **two config file trees**. Drift causes "works in staging, fails in prod."

Kiponos uses **parallel profile paths**:

- Prod: `['app']['v2']['prod']['base']`
- Staging: `['app']['v2']['staging']['base']`

Same key structure, different values — both **live**, both editable without redeploy. Promoting a config change means copying a folder in the dashboard, not merging YAML.

```java
String paymentsUrl = kiponos.path("integrations", "payments").get("url");
// Identical code in staging and prod JVMs — only profile path differs at boot
```

Boot tokens select environment; **no env vars for integration URLs**.

[kiponos.io](https://kiponos.io) · [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)