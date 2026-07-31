package io.kiponos.examples.mesh;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SreAgentPauseMeshAppLogicTest {
    @Test
    void hubKeyIsStable() {
        assertEquals("agent-tools", SreAgentPauseMeshApp.KEY);
        assertFalse(SreAgentPauseMeshApp.KEY.isBlank());
    }

    @Test
    void defaultIsNonEmpty() {
        assertFalse(SreAgentPauseMeshApp.DEFAULT.isBlank());
    }
}
