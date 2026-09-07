package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0921PmTokenMidturnAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("max-tokens", AgenticMcp0921PmTokenMidturnApp.KEY);
        assertEquals("agentic-mcp-0921-pm-token-midturn", AgenticMcp0921PmTokenMidturnApp.FOLDER);
        assertEquals("8000", AgenticMcp0921PmTokenMidturnApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0921PmTokenMidturnApp.decide(null);
        assertEquals("8000", d.value());
        assertEquals("within_token_budget", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0921PmTokenMidturnApp.decide("8000");
        assertTrue(d.proceed());
        assertEquals("within_token_budget", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0921PmTokenMidturnApp.decide("0");
        assertFalse(blocked.proceed());
        assertEquals("stop_turn_budget", blocked.action());
    }
}
