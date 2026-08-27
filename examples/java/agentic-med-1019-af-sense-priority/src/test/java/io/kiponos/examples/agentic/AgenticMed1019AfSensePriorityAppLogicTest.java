package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMed1019AfSensePriorityAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("priority", AgenticMed1019AfSensePriorityApp.KEY);
        assertEquals("agentic-med-1019-af-sense-priority", AgenticMed1019AfSensePriorityApp.FOLDER);
        assertEquals("P3", AgenticMed1019AfSensePriorityApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMed1019AfSensePriorityApp.decide(null);
        assertEquals("P3", d.value());
        assertEquals("continue_turn", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMed1019AfSensePriorityApp.decide("P3");
        assertTrue(d.proceed());
        assertEquals("continue_turn", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMed1019AfSensePriorityApp.decide("P1");
        assertFalse(blocked.proceed());
        assertEquals("abort_mid_turn_no_restart", blocked.action());
    }
}
