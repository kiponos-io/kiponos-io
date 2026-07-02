# Getting Started with Kiponos.io

Step-by-step guide: signup → first SDK connection → real-time config change → integrate into your app.

## Prerequisites

- Java 17+
- Free **[TeamPro](https://kiponos.io)** account (no credit card)
- Git (to clone this repo)

## Step 1 — Sign up and create your config

1. Go to [kiponos.io](https://kiponos.io) and sign up (TeamPro is free).
2. Follow the onboarding wizard:
   - **Application name** (e.g. `my-app`)
   - **Release** (e.g. `v1.0.0`)
   - **Environment** (e.g. `dev`)
   - **Config profile** (e.g. `base`)
3. Create config folders and items. For the golden example, ensure this tree exists (or adjust `build.gradle` profile and paths to match yours):

```
useful-urls/
  Development/
    Java/
      Frameworks-and-Libraries/
        SpringBoot/
          starter = https://start.spring.io
```

## Step 2 — Get credentials from Connect

In your Kiponos.io account, open **Connect** / SDK setup for your application environment.

Copy:

| What | Where it goes |
|------|----------------|
| **KIPONOS_ID** | JWE identity token → `build.gradle` `environment "KIPONOS_ID"` |
| **KIPONOS_ACCESS** | JWE access token → `build.gradle` `environment "KIPONOS_ACCESS"` |
| **Config profile** | Bracket path → `systemProperty "kiponos"` |

Example profile:

```
['my-app']['v1.0.0']['dev']['base']
```

## Step 3 — Run the golden example

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/golden/java
```

Edit `build.gradle` — replace the two `REPLACE_WITH_*` placeholders and confirm the profile path.

```bash
chmod +x gradlew   # if needed
./gradlew run
```

### Success looks like

```
SDK Handshake Authenticated.
Configs Ready [nodes: N]
springBootStarterURL: https://start.spring.io
```

### Common failures

| Symptom | Fix |
|---------|-----|
| Handshake / auth error | Regenerate tokens; check for trailing whitespace |
| Connected but wrong/empty value | Wrong profile path or missing config keys in dashboard |
| Still uses old shell env vars | Golden uses `JavaExec` env — should override; check placeholders were saved |

See [`skills/kiponos/references/troubleshooting.md`](../skills/kiponos/references/troubleshooting.md).

## Step 4 — Experience real-time config

1. Open your config in the Kiponos.io dashboard.
2. Change `starter` under the SpringBoot folder to a different URL.
3. Run `./gradlew run` again.

The printed value reflects the latest config — no code edit, no app restart.

For a long-running app, values update in memory automatically while connected; `kiponos.get(...)` always returns the current value.

## Step 5 — Integrate into your application

### Gradle

Add dependency:

```groovy
implementation 'io.kiponos:sdk-boot-3:4.4.0.250319'
```

Add credentials block (see [`golden/java/build.gradle`](../golden/java/build.gradle)):

```groovy
tasks.withType(JavaExec).configureEach {
    environment "KIPONOS_ID", "YOUR_TOKEN"
    environment "KIPONOS_ACCESS", "YOUR_TOKEN"
    systemProperty "kiponos", "['my-app']['v1.0.0']['dev']['base']"
}
```

Application code:

```java
Kiponos kiponos = Kiponos.createForCurrentTeam();
// use kiponos.get(...) or kiponos.path(...).get(...)
// on shutdown: kiponos.disconnect();
```

### With an AI agent

```bash
./skills/install.sh
```

Then: *"Integrate Kiponos SDK into this project per skills/kiponos/SKILL.md"*

### Production

Do not commit real tokens. Use CI/CD secrets or your platform's secret manager for `KIPONOS_ID` and `KIPONOS_ACCESS`.

## Also on dev.to

- [Kiponos.io Developer Quickstart — Java, Python, and Your First Live Config Change](https://dev.to/kiponos/kiponosio-developer-quickstart-java-python-and-your-first-live-config-change-3kjo) — developer-focused walkthrough (this repo linked at the end)
- [Getting Started with Kiponos.io](https://dev.to/kiponos/getting-started-with-kiponosio-p5k) — product tour and dashboard screenshots

## Next steps

- [`examples/comm-panel/`](../examples/comm-panel/) — richer Swing demo
- [`skills/kiponos/`](../skills/kiponos/) — full agent integration skill
- [`PUBLIC-SANDBOX.md`](PUBLIC-SANDBOX.md) — upcoming try-before-signup
- [Maven Central](https://mvnrepository.com/artifact/io.kiponos/sdk-boot-3) — latest SDK version