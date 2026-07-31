package io.kiponos.examples.mesh;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CanaryFullstackLiveAppLogicTest {
    @Test
    void hubKeyIsStable() {
        assertEquals("percent", CanaryFullstackLiveApp.KEY);
        assertFalse(CanaryFullstackLiveApp.KEY.isBlank());
    }

    @Test
    void defaultIsNonEmpty() {
        assertFalse(CanaryFullstackLiveApp.DEFAULT.isBlank());
    }
}
