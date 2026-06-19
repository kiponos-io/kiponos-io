# Spring Boot Integration

## Dependency

Same as Gradle: `io.kiponos:sdk-boot-3`.

## Bean registration

```java
@Configuration
public class KiponosConfig {

    @Bean
    public Kiponos kiponos() {
        return Kiponos.createForCurrentTeam();
    }

    @PreDestroy
    public void shutdown(@Autowired Kiponos kiponos) {
        kiponos.disconnect();
    }
}
```

Or register a `@Bean(destroyMethod = "disconnect")` if the SDK method signature allows.

## Configuration properties (optional)

Map env vars in `application.yml` for documentation only — the SDK reads `KIPONOS_ID` and `KIPONOS_ACCESS` directly from the environment, not from Spring properties by default.

Ensure `bootRun` and production JVM both have:

- `KIPONOS_ID`, `KIPONOS_ACCESS` in environment
- `-Dkiponos="['app']['rel']['env']['profile']"`

## Usage in services

```java
@Service
public class MyService {
    private final Kiponos kiponos;

    public MyService(Kiponos kiponos) {
        this.kiponos = kiponos;
    }

    public String getDbHost() {
        return kiponos.path("DB", "PostgreSql").get("host");
    }
}
```

## Live updates

Use `kiponos.afterValueChanged(...)` in a `@PostConstruct` initializer or dedicated listener bean.