package io.kiponos.examples.patterns.bridge;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class BridgeLiveImplementorGoldenTest {
    @Test @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void connectEnsureAndRun() {
        Assumptions.assumeFalse("true".equalsIgnoreCase(System.getProperty("kiponos.golden.skip")));
        String id = System.getenv("KIPONOS_ID");
        String access = System.getenv("KIPONOS_ACCESS");
        Assumptions.assumeTrue(id != null && !id.isBlank() && !id.startsWith("REPLACE_"));
        Assumptions.assumeTrue(access != null && !access.isBlank() && !access.startsWith("REPLACE_"));
        boolean required = "true".equalsIgnoreCase(System.getProperty("kiponos.golden.required"));
        Kiponos kiponos;
        try { kiponos = Kiponos.createForCurrentTeam(); }
        catch (RuntimeException ex) {
            Assumptions.assumeTrue(!required, ex.getMessage());
            Assumptions.assumeTrue(false, ex.getMessage());
            return;
        }
        try {
            Assumptions.assumeTrue(kiponos.getRootFolder() != null || !required);
            if (kiponos.getRootFolder() == null) { Assumptions.assumeTrue(false); return; }
            Folder policy = BridgeLiveImplementorApp.ensure(kiponos);
            assertNotNull(policy);
            var s = BridgeLiveImplementorApp.createImplementor(policy); assertTrue(s.deliver("a").contains(":"));
            System.out.println("GOLDEN_OK BridgeLiveImplementorApp");
        } finally { kiponos.disconnect(); }
    }
}
