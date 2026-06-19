---
title: "We Just Open-Sourced the Fastest Way to Integrate Kiponos (and Teach Your AI Agent How)"
published: false
tags: java, ai, opensource, devtools, configuration
canonical_url: https://github.com/kiponos-io/kiponos-io
---

If you've ever tried asking an AI agent to wire up a new SDK, you know the pain: half the docs are for humans, the tokens live in three different places, and the agent confidently hallucinates your config profile.

We shipped something on [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io) to fix that.

## What is Kiponos?

[Kiponos.io](https://kiponos.io) is a real-time config hub. You define variables in your browser; connected SDKs get updates over a permanent WebSocket — no redeploy, no restart, no refresh.

```java
Kiponos kiponos = Kiponos.createForCurrentTeam();
String url = kiponos.path("useful-urls", "Development", "Java", "SpringBoot").get("starter");
// always the latest value, in memory, in real time
```

## What's new in the public repo

Three pieces, one goal: **get from zero to working SDK in minutes — with or without an AI agent.**

### 1. Golden Java example (`golden/java/`)

A minimal Gradle project that connects, reads one config value, prints it, and disconnects.

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/golden/java
# open build.gradle — replace the three placeholders with tokens + profile from Kiponos.io Connect
./gradlew run
```

Credentials live in the `JavaExec` block in `build.gradle` — self-contained, isolated from your shell profile. Replace placeholders before running; don't commit real tokens.

### 2. Agent skill (`skills/kiponos/`)

An [Agent Skills](https://agentskills.io) skill for **Grok Build, Cursor, Claude Code, GitHub Copilot**, and similar tools. It encodes the full integration contract:

- `KIPONOS_ID` and `KIPONOS_ACCESS` env vars
- JVM property `-Dkiponos="['app']['release']['env']['profile']"`
- Gradle, Maven, and Spring Boot patterns
- `disconnect()` on shutdown

Install it:

```bash
./skills/install.sh
```

Then tell your agent: *"Integrate Kiponos SDK into this project."*

### 3. `AGENTS.md`

A repo-root contract any agent can read without skill installation. If your tool supports `AGENTS.md`, it already knows where to look.

## Why we built it this way

Developers using agentic tools don't need another PDF. They need a **machine-readable integration spec** plus a **runnable proof** that tokens and profile actually work.

The two real friction points are always the same:

1. **Tokens** — `KIPONOS_ID` + `KIPONOS_ACCESS` from the Connect screen
2. **Profile** — the bracket path that selects your config tree slice

The golden example proves #1 and #2. The skill teaches your agent to wire them into *your* project.

## What's coming

- A public sandbox with shared read tokens (try before signup)
- A system-wide Kiponos local agent (MCP + skill installer)
- More examples and community config trees

## Try it

- **Repo:** [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
- **Golden example:** [golden/java/](https://github.com/kiponos-io/kiponos-io/tree/master/golden/java)
- **Agent skill:** [skills/kiponos/](https://github.com/kiponos-io/kiponos-io/tree/master/skills/kiponos)
- **Sign up:** [kiponos.io](https://kiponos.io)

If you integrate Kiponos with an agent and hit a wall, open an issue — we're especially interested in what agents still get wrong.

---

*Kiponos.io — real-time config, zero latency, no restarts.*