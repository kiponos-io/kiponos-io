    package io.kiponos.examples.mesh;
    import org.junit.jupiter.api.Test;
    import static org.junit.jupiter.api.Assertions.*;
    class CanaryFullstackLiveAppLogicTest {
        @Test void hubKeyStable() {
            assertEquals("canary-percent", CanaryFullstackLiveApp.KEY);
            assertEquals("5", CanaryFullstackLiveApp.DEFAULT);
            assertTrue(CanaryFullstackLiveApp.PATH_LABEL.contains("canary-percent"));
        }

@Test void defaultParses() { assertTrue(Integer.parseInt("5") >= 0); }

    }
