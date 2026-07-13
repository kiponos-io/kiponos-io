# Contributing to kiponos-io

Thank you for helping improve Kiponos open resources.

## Ways to contribute

| Area | How |
|------|-----|
| **Golden example** | PRs to `golden/java/` — keep placeholders, no real tokens |
| **Agent skill** | Edit `skills/kiponos/` only (run `./skills/install.sh` to sync copies) |
| **Docs** | `docs/`, `README.md`, `AGENTS.md` |
| **Examples** | `examples/` — self-contained Gradle projects |
| **Issues** | Bug reports, integration failures, doc gaps |

## Before you PR

1. **Never commit** real `KIPONOS_ID`, `KIPONOS_ACCESS`, or user passwords.
2. Use placeholders in `build.gradle` (`REPLACE_WITH_*`).
3. If you change `skills/kiponos/`, run `./skills/install.sh grok-project cursor-project`.
4. Verify golden compiles: `cd golden/java && ./gradlew compileJava`.

## Questions

- [GitHub Discussions](https://github.com/kiponos-io/kiponos-io/discussions) — questions and ideas
- [GitHub Issues](https://github.com/kiponos-io/kiponos-io/issues) — bugs and concrete tasks

## Code of conduct

Be respectful. No secrets, credentials, or PII in issues or PRs.