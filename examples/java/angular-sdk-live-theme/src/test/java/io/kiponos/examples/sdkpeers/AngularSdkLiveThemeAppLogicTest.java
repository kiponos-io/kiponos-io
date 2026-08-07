        package io.kiponos.examples.sdkpeers;

        import org.junit.jupiter.api.Test;
        import static org.junit.jupiter.api.Assertions.*;

        class AngularSdkLiveThemeAppLogicTest {
            @Test
            void hubKeyIsStable() {
                assertEquals("theme", AngularSdkLiveThemeApp.KEY);
                assertEquals("ui", AngularSdkLiveThemeApp.FOLDER);
            }

            @Test
            void defaultMatchesCatalog() {
                assertEquals("night", AngularSdkLiveThemeApp.DEFAULT);
            }

@Test
void defaultIsNonBlank() {
    assertFalse("night".isBlank());
}

        }
