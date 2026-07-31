package io.kiponos.examples.mesh;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgentDebugProbeLiveAppLogicTest {
    @Test
    void hubKeyIsStable() {
        assertEquals("verbose", AgentDebugProbeLiveApp.KEY);
        assertFalse(AgentDebugProbeLiveApp.KEY.isBlank());
    }

    @Test
    void defaultIsNonEmpty() {
        assertFalse(AgentDebugProbeLiveApp.DEFAULT.isBlank());
    }
}
