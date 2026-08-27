package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMed0912MiSensePriorityAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("priority", AgenticMed0912MiSensePriorityApp.KEY);
        assertEquals("agentic-med-0912-mi-sense-priority", AgenticMed0912MiSensePriorityApp.FOLDER);
        assertEquals("P3", AgenticMed0912MiSensePriorityApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMed0912MiSensePriorityApp.decide(null);
        assertEquals("P3", d.value());
        assertEquals("continue_turn", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMed0912MiSensePriorityApp.decide("P3");
        assertTrue(d.proceed());
        assertEquals("continue_turn", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMed0912MiSensePriorityApp.decide("P1");
        assertFalse(blocked.proceed());
        assertEquals("abort_mid_turn_no_restart", blocked.action());
    }
}
