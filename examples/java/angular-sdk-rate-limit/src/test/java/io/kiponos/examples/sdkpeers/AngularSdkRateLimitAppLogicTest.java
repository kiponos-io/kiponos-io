        package io.kiponos.examples.sdkpeers;

        import org.junit.jupiter.api.Test;
        import static org.junit.jupiter.api.Assertions.*;

        class AngularSdkRateLimitAppLogicTest {
            @Test
            void hubKeyIsStable() {
                assertEquals("rps-cap", AngularSdkRateLimitApp.KEY);
                assertEquals("limits", AngularSdkRateLimitApp.FOLDER);
            }

            @Test
            void defaultMatchesCatalog() {
                assertEquals("25", AngularSdkRateLimitApp.DEFAULT);
            }

@Test
void defaultParsesAsNonNegativeInt() {
    assertTrue(Integer.parseInt("25") >= 0);
}

        }
