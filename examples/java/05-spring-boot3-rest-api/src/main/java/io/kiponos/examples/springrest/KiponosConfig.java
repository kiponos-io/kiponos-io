package io.kiponos.examples.springrest;

import io.kiponos.sdk.Kiponos;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring lifecycle for Kiponos: one bean for the process, clean disconnect on shutdown.
 */
@Configuration
public class KiponosConfig {

    private Kiponos kiponos;

    @Bean
    public Kiponos kiponos() {
        kiponos = Kiponos.createForCurrentTeam();
        return kiponos;
    }

    @PreDestroy
    public void disconnectKiponos() {
        if (kiponos != null) {
            kiponos.disconnect();
            System.out.println("Disconnected from Kiponos (@PreDestroy).");
        }
    }
}
