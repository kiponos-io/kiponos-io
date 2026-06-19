# Tokens and Config Profile

The two friction points for Kiponos onboarding.

## 1. Tokens (environment variables)

Kiponos.io issues two JWE tokens per application connection:

| Env var | Role |
|---------|------|
| `KIPONOS_ID` | Identity — who is connecting |
| `KIPONOS_ACCESS` | Access — permission to read/write config for the team |

Both are long `eyJ...` strings. The SDK sends them during the WebSocket handshake.

**Where users get them:** Kiponos.io account → application → **Connect** / SDK setup screen → copy icons (or future one-click download).

**How agents should configure them:**

- Local dev: `kiponos.local.gradle` (gitignored) or IDE run configuration env vars
- CI/CD: secret store (GitHub Actions secrets, K8s Secret, etc.)
- Never: hard-coded in committed `build.gradle` or source files

## 2. Config profile (JVM property `kiponos`)

Selects **which** config tree slice this process uses: app name, release, environment, and profile name.

Format uses bracket notation (copied from Kiponos.io):

```
['my-app']['v1.0.0']['dev']['base']
```

Gradle:

```groovy
tasks.withType(JavaExec).configureEach {
    systemProperty "kiponos", "['my-app']['v1.0.0']['dev']['base']"
}
```

Command line:

```bash
java -Dkiponos="['my-app']['v1.0.0']['dev']['base']" -jar app.jar
```

**Not the same as tokens.** Tokens authenticate; the profile selects config scope.

## Verification

If both are correct, SDK logs show:

- `SDK Handshake Authenticated`
- `Configs Ready [nodes: N]`
- Folder paths under `$.rootAccount['apps'][...]`

If handshake fails: wrong or expired tokens.

If connected but empty/wrong values: wrong profile string.