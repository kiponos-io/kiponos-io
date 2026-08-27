package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMed0922MiSensePriorityAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("priority", AgenticMed0922MiSensePriorityApp.KEY);
        assertEquals("agentic-med-0922-mi-sense-priority", AgenticMed0922MiSensePriorityApp.FOLDER);
        assertEquals("P3", AgenticMed0922MiSensePriorityApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMed0922MiSensePriorityApp.decide(null);
        assertEquals("P3", d.value());
        assertEquals("continue_turn", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMed0922MiSensePriorityApp.decide("P3");
        assertTrue(d.proceed());
        assertEquals("continue_turn", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMed0922MiSensePriorityApp.decide("P1");
        assertFalse(blocked.proceed());
        assertEquals("abort_mid_turn_no_restart", blocked.action());
    }
}
