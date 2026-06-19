# Maven Integration

## Dependency

```xml
<dependency>
    <groupId>io.kiponos</groupId>
    <artifactId>sdk-boot-3</artifactId>
    <version>4.4.0.250319</version>
</dependency>
```

Use the latest version from [Maven Central](https://mvnrepository.com/artifact/io.kiponos/sdk-boot-3) unless the project pins one.

## Run with env + profile

```bash
export KIPONOS_ID="..."
export KIPONOS_ACCESS="..."
mvn exec:java \
  -Dexec.mainClass="com.example.Main" \
  -Dkiponos="['my-app']['v1.0.0']['dev']['base']"
```

Or configure `exec-maven-plugin` in `pom.xml` with `environmentVariables` and `systemProperties`.

## Local properties file

Keep tokens in `kiponos.local.properties` (gitignored):

```properties
kiponos.id=...
kiponos.access=...
kiponos.profile=['my-app']['v1.0.0']['dev']['base']
```

Load via Maven profiles or a small properties loader — do not commit filled values.