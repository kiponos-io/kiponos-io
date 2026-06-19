# Gradle Integration

## Dependency

```groovy
dependencies {
    implementation 'io.kiponos:sdk-boot-3:4.4.0.250319' // or latest from Maven Central
}
```

## Local credentials (recommended)

`build.gradle` — apply local override if present:

```groovy
def kiponosLocal = rootProject.file('kiponos.local.gradle')
if (kiponosLocal.exists()) {
    apply from: kiponosLocal
}
```

`kiponos.local.gradle` (gitignored, user- or download-generated):

```groovy
tasks.withType(JavaExec).configureEach {
    environment "KIPONOS_ID", "<from-kiponos-io>"
    environment "KIPONOS_ACCESS", "<from-kiponos-io>"
    systemProperty "kiponos", "['my-app']['v1.0.0']['dev']['base']"
}
```

Copy from [assets/kiponos.local.gradle.example](../assets/kiponos.local.gradle.example).

## Spring Boot

Same dependency. For `bootRun`:

```groovy
tasks.named('bootRun') {
    environment "KIPONOS_ID", System.getenv("KIPONOS_ID") ?: ""
    environment "KIPONOS_ACCESS", System.getenv("KIPONOS_ACCESS") ?: ""
    systemProperty "kiponos", project.findProperty("kiponosProfile") ?: "['app']['rel']['env']['profile']"
}
```

Prefer injecting `Kiponos` as a `@Bean` with `@PreDestroy` calling `disconnect()`.

## Fat JAR / application plugin

Ensure `JavaExec` and `bootRun` both receive env + system property. CI should inject secrets, not files.