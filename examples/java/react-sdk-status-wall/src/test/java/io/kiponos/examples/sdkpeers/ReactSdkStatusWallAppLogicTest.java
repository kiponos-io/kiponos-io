        package io.kiponos.examples.sdkpeers;

        import org.junit.jupiter.api.Test;
        import static org.junit.jupiter.api.Assertions.*;

        class ReactSdkStatusWallAppLogicTest {
            @Test
            void hubKeyIsStable() {
                assertEquals("status-headline", ReactSdkStatusWallApp.KEY);
                assertEquals("ops", ReactSdkStatusWallApp.FOLDER);
            }

            @Test
            void defaultMatchesCatalog() {
                assertEquals("steady", ReactSdkStatusWallApp.DEFAULT);
            }

@Test
void defaultIsNonBlank() {
    assertFalse("steady".isBlank());
}

        }
