# Troubleshooting

## Handshake / auth failures

- Confirm `KIPONOS_ID` and `KIPONOS_ACCESS` are set in the **process** environment (not only the parent shell).
- Tokens expire — regenerate from Kiponos.io Connect screen.
- Check for trailing whitespace when pasting JWE strings.

## Connected but wrong or missing values

- Verify JVM property `kiponos` matches the profile from Kiponos.io exactly, including brackets and quotes.
- Confirm config keys/folders exist under that profile in the web UI.
- Use `path("folder", ...)` with folder names as shown in the UI, not internal JSON paths.

## Global env pollution

Local dev machines may have `KIPONOS_*` in shell profile. Gradle `JavaExec` `environment` overrides per-task — prefer explicit task config or `kiponos.local.gradle` for reproducible runs.

## SLF4J warnings

`No SLF4J providers were found` is harmless for the golden example. Add `slf4j-simple` or your project's logging backend if you want SDK logs visible.

## Agent integration failures (meta)

If an agent fails to integrate despite this skill:

1. Confirm it read `references/integration-contract.md`.
2. Confirm it did not skip the profile JVM property.
3. Confirm tokens were not committed and then redacted mid-session.
4. File an issue: https://github.com/kiponos-io/kiponos-io/issues