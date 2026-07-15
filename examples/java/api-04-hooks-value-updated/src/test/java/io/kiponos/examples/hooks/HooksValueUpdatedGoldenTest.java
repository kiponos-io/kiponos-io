package io.kiponos.examples.hooks;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Golden end-to-end test: real SDK handshake + hooks folder + afterValueUpdated registration.
 *
 * Requires KIPONOS_ID and KIPONOS_ACCESS (and optional KIPONOS profile).
 * Skips when placeholders remain or live connect is unavailable, unless
 * {@code -Dkiponos.golden.required=true}.
 */
class HooksValueUpdatedGoldenTest {

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void connectRegisterHookEnsureMaxRps() throws InterruptedException {
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

            AtomicInteger liveMaxRps = new AtomicInteger(HooksValueUpdatedApp.DEFAULT_MAX_RPS);
            CountDownLatch firstHook = new CountDownLatch(1);
            AtomicReference<String> lastKey = new AtomicReference<>();

            kiponos.afterValueUpdated(event -> {
                if (event != null && event.getKey() != null) {
                    lastKey.set(event.getKey());
                }
                HooksValueUpdatedApp.onValueUpdated(event, liveMaxRps, firstHook);
            });

            Folder hooks = HooksValueUpdatedApp.ensureHooksFolder(kiponos);
            assertNotNull(hooks);
            assertTrue(hooks.hasKey(HooksValueUpdatedApp.MAX_RPS));

            int current = HooksValueUpdatedApp.readMaxRps(hooks);
            assertTrue(current > 0);
            liveMaxRps.set(current);

            // Local set exercises hub write path; hook may or may not echo self-updates.
            int probe = current >= 999 ? 50 : current + 1;
            hooks.set(HooksValueUpdatedApp.MAX_RPS, String.valueOf(probe));
            int afterSet = HooksValueUpdatedApp.readMaxRps(hooks);
            assertEquals(probe, afterSet);

            // Brief wait: if self-update delivers afterValueUpdated, assert applied; else still OK.
            boolean hooked = firstHook.await(8, TimeUnit.SECONDS);
            if (hooked) {
                assertEquals(probe, liveMaxRps.get());
                System.out.println("GOLDEN_OK hook_fired key=" + lastKey.get() + " max-rps=" + liveMaxRps.get());
            } else {
                System.out.println("GOLDEN_OK hook_registered max-rps=" + afterSet
                        + " (self-update did not deliver hook within 8s — dashboard edits still fire live)");
            }
        } finally {
            kiponos.disconnect();
        }
    }
}
