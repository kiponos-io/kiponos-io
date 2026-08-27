package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMed1012MiSensePriorityAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("priority", AgenticMed1012MiSensePriorityApp.KEY);
        assertEquals("agentic-med-1012-mi-sense-priority", AgenticMed1012MiSensePriorityApp.FOLDER);
        assertEquals("P3", AgenticMed1012MiSensePriorityApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMed1012MiSensePriorityApp.decide(null);
        assertEquals("P3", d.value());
        assertEquals("continue_turn", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMed1012MiSensePriorityApp.decide("P3");
        assertTrue(d.proceed());
        assertEquals("continue_turn", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMed1012MiSensePriorityApp.decide("P1");
        assertFalse(blocked.proceed());
        assertEquals("abort_mid_turn_no_restart", blocked.action());
    }
}
