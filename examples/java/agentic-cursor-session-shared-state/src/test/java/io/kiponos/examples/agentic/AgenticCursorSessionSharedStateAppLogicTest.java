package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticCursorSessionSharedStateAppLogicTest {
    @Test
    void hubKeyIsStable() {
        assertEquals("session-posture", AgenticCursorSessionSharedStateApp.KEY);
        assertEquals("agentic-cursor-session-shared-state", AgenticCursorSessionSharedStateApp.FOLDER);
        assertFalse(AgenticCursorSessionSharedStateApp.KEY.isBlank());
    }

    @Test
    void defaultIsNonEmpty() {
        assertFalse(AgenticCursorSessionSharedStateApp.DEFAULT.isBlank());
    }
}
