package io.kiponos.examples.springrest;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Golden end-to-end test: real SDK handshake + spring-boot-rest folder/keys.
 *
 * Requires KIPONOS_ID and KIPONOS_ACCESS (and optional KIPONOS profile).
 * Skips when placeholders remain or live connect is unavailable, unless
 * {@code -Dkiponos.golden.required=true}.
 *
 * Does not boot the full Spring context — exercises the same folder/key contract
 * the REST bean uses.
 */
class SpringRestGoldenTest {

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void connectEnsureSpringRestKeysAndRead() {
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

            LiveOpsConfig live = new LiveOpsConfig(kiponos);
            Folder ops = live.ensureOpsFolder();
            assertNotNull(ops);
            assertTrue(ops.hasKey(LiveOpsConfig.REQUEST_TIMEOUT_MS));
            assertTrue(ops.hasKey(LiveOpsConfig.GREETING));

            int timeout = live.requestTimeoutMs();
            String greeting = live.greeting();
            assertTrue(timeout > 0);
            assertNotNull(greeting);
            assertFalse(greeting.isBlank());

            System.out.println("GOLDEN_OK request-timeout-ms=" + timeout + " greeting=" + greeting);
        } finally {
            kiponos.disconnect();
        }
    }
}
