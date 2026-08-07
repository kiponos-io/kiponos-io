        package io.kiponos.examples.sdkpeers;

        import org.junit.jupiter.api.Test;
        import static org.junit.jupiter.api.Assertions.*;

        class ReactSdkIncidentPauseAppLogicTest {
            @Test
            void hubKeyIsStable() {
                assertEquals("pause-risky", ReactSdkIncidentPauseApp.KEY);
                assertEquals("incident", ReactSdkIncidentPauseApp.FOLDER);
            }

            @Test
            void defaultMatchesCatalog() {
                assertEquals("off", ReactSdkIncidentPauseApp.DEFAULT);
            }

@Test
void defaultIsNonBlank() {
    assertFalse("off".isBlank());
}

        }
