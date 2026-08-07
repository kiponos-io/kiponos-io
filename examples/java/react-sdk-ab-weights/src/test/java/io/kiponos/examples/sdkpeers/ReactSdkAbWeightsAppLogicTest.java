        package io.kiponos.examples.sdkpeers;

        import org.junit.jupiter.api.Test;
        import static org.junit.jupiter.api.Assertions.*;

        class ReactSdkAbWeightsAppLogicTest {
            @Test
            void hubKeyIsStable() {
                assertEquals("ab-weights", ReactSdkAbWeightsApp.KEY);
                assertEquals("experiments", ReactSdkAbWeightsApp.FOLDER);
            }

            @Test
            void defaultMatchesCatalog() {
                assertEquals("50,50", ReactSdkAbWeightsApp.DEFAULT);
            }

@Test
void defaultIsNonBlank() {
    assertFalse("50,50".isBlank());
}

        }
