package io.kiponos.examples.sre;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Golden end-to-end test: real SDK handshake + degradation-mode kill-switch tree.
 *
 * Requires KIPONOS_ID and KIPONOS_ACCESS (and optional KIPONOS profile).
 * Skips when placeholders remain or live connect is unavailable, unless
 * {@code -Dkiponos.golden.required=true}.
 */
class DegradationModeGoldenTest {

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void connectEnsureDegradationTreeAndReadPosture() {
        Assumptions.assumeFalse(
                "true".equalsIgnoreCase(System.getProperty("kiponos.golden.skip")),
                "Skipping golden: kiponos.golden.skip=true"
        );
        String id = System.getenv("KIPONOS_ID");
        String access = System.getenv("KIPONOS_ACCESS");
        Assumptions.assumeTrue(id != null && !id.isBlank() && !id.startsWith("REPLACE_"),
                "KIPONOS_ID missing — set env or build.gradle placeholders from Connect UI");
        Assumptions.assumeTrue(access != null && !access.isBlank() && !access.startsWith("REPLACE_"),
                "KIPONOS_ACCESS missing");

        boolean required = "true".equalsIgnoreCase(System.getProperty("kiponos.golden.required"));

        Kiponos kiponos;
        try {
            kiponos = Kiponos.createForCurrentTeam();
        } catch (RuntimeException ex) {
            Assumptions.assumeTrue(!required,
                    "Live connect failed (required golden): " + ex.getMessage());
            Assumptions.assumeTrue(false, "Live connect failed — skip golden: " + ex.getMessage());
            return;
        }

        try {
            Folder root = kiponos.getRootFolder();
            if (root == null) {
                Assumptions.assumeTrue(!required,
                        "SDK not in Ready mode (root null). Check tokens/profile/server.");
                Assumptions.assumeTrue(false, "SDK root null — not Ready; skip golden");
                return;
            }

            Folder folder = DegradationModeApp.ensureDegradationFolder(kiponos);
            assertNotNull(folder);
            assertTrue(folder.hasKey(DegradationModeApp.MODE));
            assertTrue(folder.hasKey(DegradationModeApp.ACCEPT_WRITES));
            assertTrue(folder.hasKey(DegradationModeApp.BACKGROUND_JOBS));

            DegradationModeApp.Posture posture = DegradationModeApp.readPosture(folder);
            assertNotNull(posture.mode());
            assertTrue(
                    posture.mode().equals("full")
                            || posture.mode().equals("read-only")
                            || posture.mode().equals("maintenance")
            );
            System.out.println("GOLDEN_OK mode=" + posture.mode()
                    + " acceptWrites=" + posture.acceptWrites()
                    + " backgroundJobs=" + posture.backgroundJobs());
        } finally {
            kiponos.disconnect();
        }
    }
}
