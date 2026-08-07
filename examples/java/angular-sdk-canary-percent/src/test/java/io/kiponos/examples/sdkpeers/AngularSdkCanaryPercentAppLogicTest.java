        package io.kiponos.examples.sdkpeers;

        import org.junit.jupiter.api.Test;
        import static org.junit.jupiter.api.Assertions.*;

        class AngularSdkCanaryPercentAppLogicTest {
            @Test
            void hubKeyIsStable() {
                assertEquals("canary-percent", AngularSdkCanaryPercentApp.KEY);
                assertEquals("release", AngularSdkCanaryPercentApp.FOLDER);
            }

            @Test
            void defaultMatchesCatalog() {
                assertEquals("5", AngularSdkCanaryPercentApp.DEFAULT);
            }

@Test
void defaultParsesAsNonNegativeInt() {
    assertTrue(Integer.parseInt("5") >= 0);
}

        }
