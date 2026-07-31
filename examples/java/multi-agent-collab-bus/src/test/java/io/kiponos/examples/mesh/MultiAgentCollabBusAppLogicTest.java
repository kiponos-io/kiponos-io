package io.kiponos.examples.mesh;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MultiAgentCollabBusAppLogicTest {
    @Test
    void hubKeyIsStable() {
        assertEquals("handoff-ticket", MultiAgentCollabBusApp.KEY);
        assertFalse(MultiAgentCollabBusApp.KEY.isBlank());
    }

    @Test
    void defaultIsNonEmpty() {
        assertFalse(MultiAgentCollabBusApp.DEFAULT.isBlank());
    }
}
