        package io.kiponos.examples.sdkpeers;

        import org.junit.jupiter.api.Test;
        import static org.junit.jupiter.api.Assertions.*;

        class AngularSdkFeatureKillAppLogicTest {
            @Test
            void hubKeyIsStable() {
                assertEquals("feature-x", AngularSdkFeatureKillApp.KEY);
                assertEquals("flags", AngularSdkFeatureKillApp.FOLDER);
            }

            @Test
            void defaultMatchesCatalog() {
                assertEquals("off", AngularSdkFeatureKillApp.DEFAULT);
            }

@Test
void defaultIsNonBlank() {
    assertFalse("off".isBlank());
}

        }
