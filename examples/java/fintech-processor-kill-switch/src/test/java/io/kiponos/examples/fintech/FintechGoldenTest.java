package io.kiponos.examples.fintech;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Golden E2E: real handshake + fintech processor folder/key.
 * Skips without tokens unless -Dkiponos.golden.required=true.
 */
class FintechGoldenTest {

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void connectEnsureProcessorAndAuthorizeDemo() {
        Assumptions.assumeFalse(
                "true".equalsIgnoreCase(System.getProperty("kiponos.golden.skip")),
                "Skipping golden: kiponos.golden.skip=true"
        );
        String id = System.getenv("KIPONOS_ID");
        String access = System.getenv("KIPONOS_ACCESS");
        Assumptions.assumeTrue(id != null && !id.isBlank() && !id.startsWith("REPLACE_"),
                "KIPONOS_ID missing");
        Assumptions.assumeTrue(access != null && !access.isBlank() && !access.startsWith("REPLACE_"),
                "KIPONOS_ACCESS missing");

        boolean required = "true".equalsIgnoreCase(System.getProperty("kiponos.golden.required"));

        Kiponos kiponos;
        try {
            kiponos = Kiponos.createForCurrentTeam();
        } catch (RuntimeException ex) {
            Assumptions.assumeTrue(!required, "Live connect failed: " + ex.getMessage());
            Assumptions.assumeTrue(false, "Live connect failed — skip: " + ex.getMessage());
            return;
        }

        try {
            Folder root = kiponos.getRootFolder();
            if (root == null) {
                Assumptions.assumeTrue(!required, "SDK root null (not Ready)");
                Assumptions.assumeTrue(false, "SDK root null — skip golden");
                return;
            }

            Folder processor = FintechProcessorKillSwitchApp.ensureProcessorFolder(
                    kiponos, "acquirer-alpha");
            assertNotNull(processor);
            assertTrue(processor.hasKey("accept-new-auth"));

            var decision = FintechProcessorKillSwitchApp.authorizeDemo(
                    processor, "golden-txn", 999L);
            assertNotNull(decision.status());
            System.out.println("GOLDEN_OK accept="
                    + FintechProcessorKillSwitchApp.readAcceptNewAuth(processor)
                    + " decision=" + decision.status());
        } finally {
            kiponos.disconnect();
        }
    }
}
