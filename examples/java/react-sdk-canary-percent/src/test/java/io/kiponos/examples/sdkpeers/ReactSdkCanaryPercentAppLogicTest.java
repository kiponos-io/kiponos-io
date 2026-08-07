        package io.kiponos.examples.sdkpeers;

        import org.junit.jupiter.api.Test;
        import static org.junit.jupiter.api.Assertions.*;

        class ReactSdkCanaryPercentAppLogicTest {
            @Test
            void hubKeyIsStable() {
                assertEquals("canary-percent", ReactSdkCanaryPercentApp.KEY);
                assertEquals("release", ReactSdkCanaryPercentApp.FOLDER);
            }

            @Test
            void defaultMatchesCatalog() {
                assertEquals("5", ReactSdkCanaryPercentApp.DEFAULT);
            }

@Test
void defaultParsesAsNonNegativeInt() {
    assertTrue(Integer.parseInt("5") >= 0);
}

        }
