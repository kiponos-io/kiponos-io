        package io.kiponos.examples.sdkpeers;

        import org.junit.jupiter.api.Test;
        import static org.junit.jupiter.api.Assertions.*;

        class AngularSdkAbWeightsAppLogicTest {
            @Test
            void hubKeyIsStable() {
                assertEquals("ab-weights", AngularSdkAbWeightsApp.KEY);
                assertEquals("experiments", AngularSdkAbWeightsApp.FOLDER);
            }

            @Test
            void defaultMatchesCatalog() {
                assertEquals("70,30", AngularSdkAbWeightsApp.DEFAULT);
            }

@Test
void defaultIsNonBlank() {
    assertFalse("70,30".isBlank());
}

        }
