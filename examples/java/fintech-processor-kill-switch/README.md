# Example — FinTech processor kill switch

| | |
|--|--|
| **ID** | `fintech-processor-kill-switch` |
| **Level** | Intro (industry-deep variant of `01-standalone-3am-kill-switch`) |
| **App shape** | Standalone `main` (no framework) |
| **Industry** | FinTech / payments / card acquirer rails |
| **Pain** | “Stop a flaky acquirer without a deploy” |
| **SDK** | `createForCurrentTeam`, `folderOrCreate`, `set` / `get`, `disconnect` |

## Business problem

A card processor (acquirer rail) is timing out. New authorizations must stop **now** —
compliance and risk will not wait for a PR that flips a boolean in `application-prod.yml`.

## What this example does

Connects to Kiponos and reads:

```text
examples / fintech / processors / {acquirerId} / accept-new-auth
examples / fintech / processors / {acquirerId} / disable-reason
```

- `accept-new-auth` truthy → demo auth path **APPROVED_DEMO**
- falsy → **REFUSED** with optional reason (safe posture for new auths)

Default acquirer id: `acquirer-alpha` (override with CLI arg).

## Run (credentials stay inside Gradle’s child JVM)

**Do not** rely on random `export KIPONOS_*` in your shell (easy to mix Family-Agent vs my-app).

1. Copy `kiponos.local.env.example` → `kiponos.local.env` (gitignored) and fill Connect tokens for **my-app** (product demo profile).  
2. Gradle injects those only into `run` / `test` processes:

```bash
cd examples/java/fintech-processor-kill-switch
./gradlew run
# or: ./gradlew run --args='acquirer-beta'
./gradlew test
```

Public clones without `kiponos.local.env` keep placeholders and **skip** live golden tests.

Agent / dual-outlet / OTP work uses a **different** profile (`Family-Agent`) via Python wrappers — never put Family-Agent tokens in this example’s `kiponos.local.env`.

## Dashboard exercise

1. Open Kiponos → profile path above  
2. `examples → fintech → processors → acquirer-alpha`  
3. Set `accept-new-auth` to `no`, optional `disable-reason` = `acquirer_timeout`  
4. `./gradlew run` again → **REFUSED** without code change  

## Related

- Sibling intro: [`01-standalone-3am-kill-switch`](../01-standalone-3am-kill-switch/)  
- Catalog: [`examples/CATALOG.md`](../../CATALOG.md)  
- Public repo: [kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
