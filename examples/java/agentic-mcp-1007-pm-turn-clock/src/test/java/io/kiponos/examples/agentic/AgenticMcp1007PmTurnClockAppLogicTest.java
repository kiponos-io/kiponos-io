package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp1007PmTurnClockAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("max-tokens", AgenticMcp1007PmTurnClockApp.KEY);
        assertEquals("agentic-mcp-1007-pm-turn-clock", AgenticMcp1007PmTurnClockApp.FOLDER);
        assertEquals("8000", AgenticMcp1007PmTurnClockApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp1007PmTurnClockApp.decide(null);
        assertEquals("8000", d.value());
        assertEquals("within_token_budget", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp1007PmTurnClockApp.decide("8000");
        assertTrue(d.proceed());
        assertEquals("within_token_budget", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp1007PmTurnClockApp.decide("0");
        assertFalse(blocked.proceed());
        assertEquals("stop_turn_budget", blocked.action());
    }
}
