package io.kiponos.examples.multienv;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Golden E2E: real SDK handshake + multi-env folder/keys under the active profile.
 *
 * Requires KIPONOS_ID and KIPONOS_ACCESS. Skips when placeholders remain unless
 * {@code -Dkiponos.golden.required=true}.
 */
class MultiEnvGoldenTest {

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void connectEnsureMultiEnvKeysAndRead() {
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
        String profile = System.getProperty("kiponos", "(unset)");

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

            Folder multiEnv = MultiEnvProfileApp.ensureMultiEnvFolder(kiponos, profile);
            assertNotNull(multiEnv);
            assertTrue(multiEnv.hasKey("env-label"));
            assertTrue(multiEnv.hasKey("api-base-url"));

            String label = MultiEnvProfileApp.readEnvLabel(multiEnv);
            String api = MultiEnvProfileApp.readApiBaseUrl(multiEnv);
            assertFalse(label.isBlank());
            assertTrue(api.startsWith("http"));
            System.out.println("GOLDEN_OK profile=" + profile + " env-label=" + label + " api-base-url=" + api);
        } finally {
            kiponos.disconnect();
        }
    }
}
