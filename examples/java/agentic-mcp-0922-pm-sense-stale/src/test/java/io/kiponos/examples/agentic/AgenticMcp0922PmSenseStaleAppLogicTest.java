package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0922PmSenseStaleAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("priority", AgenticMcp0922PmSenseStaleApp.KEY);
        assertEquals("agentic-mcp-0922-pm-sense-stale", AgenticMcp0922PmSenseStaleApp.FOLDER);
        assertEquals("P3", AgenticMcp0922PmSenseStaleApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0922PmSenseStaleApp.decide(null);
        assertEquals("P3", d.value());
        assertEquals("continue_turn", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0922PmSenseStaleApp.decide("P3");
        assertTrue(d.proceed());
        assertEquals("continue_turn", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0922PmSenseStaleApp.decide("P1");
        assertFalse(blocked.proceed());
        assertEquals("abort_mid_turn_no_restart", blocked.action());
    }
}
