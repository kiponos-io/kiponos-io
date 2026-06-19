# Gradle Integration

## Dependency

```groovy
dependencies {
    implementation 'io.kiponos:sdk-boot-3:4.4.0.250319' // or latest from Maven Central
}
```

## Credentials in build.gradle (golden pattern)

Replace placeholders in the `JavaExec` block. Reference: [`golden/java/build.gradle`](../../../golden/java/build.gradle).

```groovy
// Kiponos credentials — replace from Kiponos.io Connect screen before running.
tasks.withType(JavaExec).configureEach {
    environment "KIPONOS_ID", "REPLACE_WITH_KIPONOS_ID_FROM_ACCOUNT"
    environment "KIPONOS_ACCESS", "REPLACE_WITH_KIPONOS_ACCESS_FROM_ACCOUNT"
    systemProperty "kiponos", "['your-app']['your-release']['your-env']['your-profile']"
}
```

This keeps the Gradle run task self-contained and unaffected by global shell env vars.

For **production** or **CI**, use secrets (env vars from the runner) instead of hard-coded tokens in committed files.

## Spring Boot

Same dependency. For `bootRun`:

```groovy
tasks.named('bootRun') {
    environment "KIPONOS_ID", System.getenv("KIPONOS_ID") ?: "REPLACE_WITH_KIPONOS_ID_FROM_ACCOUNT"
    environment "KIPONOS_ACCESS", System.getenv("KIPONOS_ACCESS") ?: "REPLACE_WITH_KIPONOS_ACCESS_FROM_ACCOUNT"
    systemProperty "kiponos", project.findProperty("kiponosProfile") ?: "['app']['rel']['env']['profile']"
}
```

Prefer injecting `Kiponos` as a `@Bean` with `@PreDestroy` calling `disconnect()`.

## Fat JAR / application plugin

Ensure `JavaExec` and `bootRun` both receive env + system property. CI should inject secrets, not committed tokens.