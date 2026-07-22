package io.kiponos.examples.patterns.adapter;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class AdapterLivePspGoldenTest {
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
            Folder policy = AdapterLivePspApp.ensure(kiponos);
            assertNotNull(policy);
            var r = AdapterLivePspApp.charge(policy, 100L); assertFalse(r.receipt().isBlank());
            System.out.println("GOLDEN_OK AdapterLivePspApp");
        } finally { kiponos.disconnect(); }
    }
}
