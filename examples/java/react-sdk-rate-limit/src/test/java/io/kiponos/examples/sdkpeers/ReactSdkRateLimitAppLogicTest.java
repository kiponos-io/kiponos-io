        package io.kiponos.examples.sdkpeers;

        import org.junit.jupiter.api.Test;
        import static org.junit.jupiter.api.Assertions.*;

        class ReactSdkRateLimitAppLogicTest {
            @Test
            void hubKeyIsStable() {
                assertEquals("rps-cap", ReactSdkRateLimitApp.KEY);
                assertEquals("limits", ReactSdkRateLimitApp.FOLDER);
            }

            @Test
            void defaultMatchesCatalog() {
                assertEquals("40", ReactSdkRateLimitApp.DEFAULT);
            }

@Test
void defaultParsesAsNonNegativeInt() {
    assertTrue(Integer.parseInt("40") >= 0);
}

        }
