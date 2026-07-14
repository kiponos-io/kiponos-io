package io.kiponos.examples.springrest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 3 REST — live ops knobs via Kiponos bean.
 *
 * Classic pain: {@code @Value} / {@code application.yml} freezes timeouts and
 * messages until the next restart (or a brittle {@code @RefreshScope} churn).
 *
 * Kiponos fix: a singleton bean + {@code @PreDestroy} disconnect; each request
 * reads the hub-backed cache. Ops flips the dashboard — no redeploy.
 */
@SpringBootApplication
public class RestApiApp {

    public static void main(String[] args) {
        SpringApplication.run(RestApiApp.class, args);
    }
}
