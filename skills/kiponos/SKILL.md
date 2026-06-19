---
name: kiponos
description: >
  Integrates the Kiponos.io real-time config SDK into Java applications (Gradle,
  Maven, Spring Boot). Adds dependencies, wires KIPONOS_ID and KIPONOS_ACCESS env
  vars, sets the kiponos JVM profile property, and implements createForCurrentTeam,
  path/get access, and disconnect lifecycle. Use when the user mentions Kiponos,
  kiponos.io, real-time config, config hub, KIPONOS_ID, KIPONOS_ACCESS, or asks
  to integrate, connect, or onboard Kiponos SDK. Slash-style: /kiponos.
license: Apache-2.0
metadata:
  author: kiponos-io
  version: "1.0.0"
  homepage: https://github.com/kiponos-io/kiponos-io
compatibility: Requires network access to kiponos.io, Java 17+, Gradle or Maven.
---

# Kiponos SDK Integration

Integrate [Kiponos.io](https://kiponos.io) real-time config into the user's **existing** Java project. Do not only scaffold a demo — patch their real build and application code.

## Before you change anything

1. Read [references/integration-contract.md](references/integration-contract.md) — the non-negotiable contract.
2. If tokens or profile are missing, ask the user to copy them from their Kiponos.io account **Connect** screen and paste into `build.gradle` placeholders (see `golden/java/build.gradle`). Never invent tokens.
3. Optionally verify connectivity using the golden example at `golden/java/` in this repo (`./gradlew run`).

## Integration workflow

### 1. Detect build system

| Build | Reference |
|-------|-----------|
| Gradle | [references/gradle.md](references/gradle.md) |
| Maven | [references/maven.md](references/maven.md) |
| Spring Boot | [references/spring-boot.md](references/spring-boot.md) |

Add dependency: `io.kiponos:sdk-boot-3:<version>` — use the latest from [Maven Central](https://mvnrepository.com/artifact/io.kiponos/sdk-boot-3) unless the project pins a version.

### 2. Wire authentication (two inputs)

| Input | Mechanism | Example |
|-------|-----------|---------|
| `KIPONOS_ID` | Environment variable | JWE from Kiponos.io Connect UI |
| `KIPONOS_ACCESS` | Environment variable | JWE from Kiponos.io Connect UI |
| Config profile | JVM system property `kiponos` | `['my-app']['v1.0.0']['dev']['base']` |

**Gradle run tasks:** add a `tasks.withType(JavaExec)` block with `environment` + `systemProperty "kiponos"` — use placeholders until the user supplies real values. See [`golden/java/build.gradle`](../../golden/java/build.gradle).

**Production:** use env vars from secrets manager / K8s secrets — never commit real tokens to git.

### 3. Add application code

```java
import io.kiponos.sdk.Kiponos;

// One instance per application (or Spring bean)
Kiponos kiponos = Kiponos.createForCurrentTeam();

// Root-level key
String value = kiponos.get("some-key");

// Nested folders
String url = kiponos.path("folder-a", "folder-b").get("item-key");
int port = kiponos.path("DB", "PostgreSql").getInt("port");

// On shutdown (main, @PreDestroy, or shutdown hook)
kiponos.disconnect();
```

### 4. Verify

Run the app (or `./gradlew run` on golden). Expect SDK logs showing WebSocket connect, config nodes loaded, then your printed value. If handshake fails, re-check env vars and profile string — see [references/troubleshooting.md](references/troubleshooting.md).

### 5. Summarize for the user

Report: dependency added, where tokens/profile are configured, code entry point, and how to test a live config change in the Kiponos.io UI.

## Rules

- **Never commit** real `KIPONOS_ID` or `KIPONOS_ACCESS` values (placeholders in public examples are fine).
- **Always call** `disconnect()` on application shutdown.
- **Prefer** `path("folder", ...).get("key")` over hard-coding JSON paths — the SDK resolves from the profile root.
- **Match** existing project style (package layout, DI framework, logging).

## References (read on demand)

- [integration-contract.md](references/integration-contract.md) — agent-readable spec
- [tokens-and-profile.md](references/tokens-and-profile.md) — what the two auth inputs mean
- [troubleshooting.md](references/troubleshooting.md) — common failures