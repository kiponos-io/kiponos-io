package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticSensesMidTurnAppLogicTest {
    @Test
    void hubKeyIsStable() {
        assertEquals("priority", AgenticSensesMidTurnApp.KEY);
        assertEquals("agentic-senses-mid-turn", AgenticSensesMidTurnApp.FOLDER);
        assertFalse(AgenticSensesMidTurnApp.KEY.isBlank());
    }

    @Test
    void defaultIsNonEmpty() {
        assertFalse(AgenticSensesMidTurnApp.DEFAULT.isBlank());
    }
}
