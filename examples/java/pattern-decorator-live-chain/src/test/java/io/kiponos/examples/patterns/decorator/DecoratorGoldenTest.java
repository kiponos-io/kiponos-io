package io.kiponos.examples.patterns.decorator;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DecoratorGoldenTest {

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void connectEnsureChainAndExecute() {
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

            Folder policy = DecoratorLiveChainApp.ensureDecoratorFolder(kiponos);
            assertNotNull(policy);
            assertTrue(policy.hasKey("chain"));

            var result = DecoratorLiveChainApp.execute(policy, "/api/golden");
            assertNotNull(result.body());
            assertFalse(result.chainApplied().isBlank());
            System.out.println("GOLDEN_OK chain=" + result.chainApplied() + " body=" + result.body());
        } finally {
            kiponos.disconnect();
        }
    }
}
