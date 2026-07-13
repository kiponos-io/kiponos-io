# Kiponos examples

Runnable scenarios for the public [Kiponos.io](https://kiponos.io) SDK.

| Start here | Path |
|------------|------|
| **Catalog (full backlog)** | [CATALOG.md](CATALOG.md) |
| **Golden smoke** | [`../golden/java`](../golden/java) |
| **Example 01 — 3am kill switch** | [`java/01-standalone-3am-kill-switch`](java/01-standalone-3am-kill-switch) |

## How to run any Java example

```bash
cd examples/java/<id>
export KIPONOS_ID='…'
export KIPONOS_ACCESS='…'
export KIPONOS="['my-app']['v1.0.0']['dev']['base']"   # your profile
./gradlew test run
```

Never commit real tokens. Placeholders stay in git; local env wins.

Each example has a `README.md` (problem → Kiponos fix → run) and tests. Prefer the catalog when choosing which scenario matches your stack.
