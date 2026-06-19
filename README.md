# Kiponos.io

**Real-time config for Java apps — change values in your browser, see them instantly in running code. No restart. No redeploy.**

[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](golden/java/)
[![Agent Skills](https://img.shields.io/badge/Agent%20Skills-agentskills.io-purple.svg)](skills/kiponos/)
[![TeamPro](https://img.shields.io/badge/TeamPro-Free-green.svg)](https://kiponos.io)

---

## Start here (5 minutes)

### 1. Sign up — free TeamPro

Create a free account at **[kiponos.io](https://kiponos.io)** (TeamPro plan, no credit card). Complete the onboarding wizard: app name, release, environment, and first config items.

### 2. Run the golden example

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/golden/java
```

Open `build.gradle` and replace the three placeholders in the `JavaExec` block with values from your account **Connect** screen:

- `REPLACE_WITH_KIPONOS_ID_FROM_ACCOUNT`
- `REPLACE_WITH_KIPONOS_ACCESS_FROM_ACCOUNT`
- Profile defaults to `['my-app']['v1.0.0']['dev']['base']` — change if yours differs

```bash
./gradlew run
```

You should see `springBootStarterURL: https://start.spring.io` and SDK logs showing `SDK Handshake Authenticated`.

**Full walkthrough:** [`docs/GETTING-STARTED.md`](docs/GETTING-STARTED.md)

### 3. See real-time config in action

1. Keep the app running (or run again).
2. In your Kiponos.io dashboard, change the `starter` value under `useful-urls → Development → Java → Frameworks-and-Libraries → SpringBoot`.
3. Run `./gradlew run` again — the printed URL updates. No code change. No restart.

That is the Kiponos moment.

### 4. Integrate into your project (with or without AI)

| Path | How |
|------|-----|
| **AI agent** | `./skills/install.sh` then ask: *"Integrate Kiponos SDK using skills/kiponos/SKILL.md"* |
| **Manual** | Follow [`docs/GETTING-STARTED.md`](docs/GETTING-STARTED.md) integration section |
| **Agent contract** | Read [`AGENTS.md`](AGENTS.md) |

---

## What is Kiponos?

Kiponos is a real-time config hub. Your application connects via the Java SDK over a permanent WebSocket. Any change you make in the web dashboard is pushed instantly to every connected SDK — in memory, at runtime.

```java
Kiponos kiponos = Kiponos.createForCurrentTeam();
String url = kiponos.path("DB", "PostgreSql").get("host");
int port = kiponos.path("DB", "PostgreSql").getInt("port");
kiponos.disconnect(); // on shutdown
```

**SDK:** [Maven Central `io.kiponos:sdk-boot-3`](https://mvnrepository.com/artifact/io.kiponos/sdk-boot-3)

---

## Repository map

| Path | What |
|------|------|
| [`golden/java/`](golden/java/) | Minimal runnable Gradle smoke test |
| [`skills/kiponos/`](skills/kiponos/) | Agent skill (Grok, Cursor, Claude, Copilot) |
| [`AGENTS.md`](AGENTS.md) | Machine-readable integration contract |
| [`docs/GETTING-STARTED.md`](docs/GETTING-STARTED.md) | Step-by-step human onboarding |
| [`examples/comm-panel/`](examples/comm-panel/) | Swing demo — live window position/title from config |
| [`docs/PUBLIC-SANDBOX.md`](docs/PUBLIC-SANDBOX.md) | Planned try-before-signup public config (coming) |

**Releases:** [GitHub Releases](https://github.com/kiponos-io/kiponos-io/releases) — downloadable `golden-java.zip`

---

## Coming soon

- **Public sandbox** — read-only tokens in golden so you can `./gradlew run` before signup ([details](docs/PUBLIC-SANDBOX.md))
- **Community config tree** — shared live values developers worldwide can read
- **Config locks** — team admins lock folders, keys, and values (UI + server ready; metadata persistence in progress)
- **Kiponos local agent** — MCP + skill installer for one-click integration

Questions? [Open a Discussion](https://github.com/kiponos-io/kiponos-io/discussions) or [an issue](https://github.com/kiponos-io/kiponos-io/issues).

---

[Join Us — free TeamPro at kiponos.io](https://kiponos.io)