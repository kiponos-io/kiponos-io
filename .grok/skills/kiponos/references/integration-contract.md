# Kiponos SDK Integration Contract

Machine-readable contract for agents integrating Kiponos into a Java application.

## SDK coordinates

```
groupId:    io.kiponos
artifactId: sdk-boot-3
repository: Maven Central
```

Factory: `Kiponos.createForCurrentTeam()` — uses current team from authenticated session.

## Required runtime configuration

### Environment variables

| Variable | Required | Description |
|----------|----------|-------------|
| `KIPONOS_ID` | Yes | JWE identity token from Kiponos.io account |
| `KIPONOS_ACCESS` | Yes | JWE access token from Kiponos.io account |

### JVM system property

| Property | Required | Format | Example |
|----------|----------|--------|---------|
| `kiponos` | Yes | Bracket path: app, release, env, profile | `['my-app']['v1.0.0']['dev']['base']` |

Passed as `-Dkiponos="['app']['rel']['env']['cfg']"` or Gradle `systemProperty "kiponos", "..."`.

The SDK maps this to the config tree root, e.g.:

```
$.rootAccount['apps']['my-app']['rels']['v1.0.0']['envs']['dev']['cfgs']['base']
```

## API patterns

```java
Kiponos kiponos = Kiponos.createForCurrentTeam();

// Key at profile root
String v = kiponos.get("key");
int n = kiponos.getInt("key");

// Nested folders (each arg is a folder segment under the profile root)
KiponosFolder folder = kiponos.path("a", "b", "c");
String v2 = folder.get("item-key");

// Real-time change listener (optional)
kiponos.afterValueChanged(change -> { /* react to live updates */ });

// Required on shutdown
kiponos.disconnect();
```

## Lifecycle

1. `createForCurrentTeam()` — connects WebSocket to `wss://kiponos.io/api/io-kiponos-sdk`, loads config tree.
2. Reads are always from in-memory latest values (no refresh call).
3. `disconnect()` — closes session, stops reconnect workers.

## Build-tool checklist

- [ ] `io.kiponos:sdk-boot-3` dependency added
- [ ] `KIPONOS_ID` and `KIPONOS_ACCESS` available at runtime (env or run-task config)
- [ ] `-Dkiponos=...` set for the JVM process
- [ ] `createForCurrentTeam()` called once (singleton / bean)
- [ ] `disconnect()` on shutdown
- [ ] Tokens not committed to git

## Golden reference

Runnable minimal example: `golden/java/` in [kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io).