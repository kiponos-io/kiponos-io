package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticSensesMidTurnAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("priority", AgenticSensesMidTurnApp.KEY);
        assertEquals("agentic-senses-mid-turn", AgenticSensesMidTurnApp.FOLDER);
        assertEquals("P3", AgenticSensesMidTurnApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticSensesMidTurnApp.decide(null);
        assertEquals("P3", d.value());
        assertEquals("continue_turn", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticSensesMidTurnApp.decide("P3");
        assertTrue(d.proceed());
        assertEquals("continue_turn", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticSensesMidTurnApp.decide("P1");
        assertFalse(blocked.proceed());
        assertEquals("abort_mid_turn_no_restart", blocked.action());
    }
}
