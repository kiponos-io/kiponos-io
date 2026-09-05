package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev1027AmSensePriorityAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("priority", AgenticDev1027AmSensePriorityApp.KEY);
        assertEquals("agentic-dev-1027-am-sense-priority", AgenticDev1027AmSensePriorityApp.FOLDER);
        assertEquals("P3", AgenticDev1027AmSensePriorityApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev1027AmSensePriorityApp.decide(null);
        assertEquals("P3", d.value());
        assertEquals("continue_turn", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev1027AmSensePriorityApp.decide("P3");
        assertTrue(d.proceed());
        assertEquals("continue_turn", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev1027AmSensePriorityApp.decide("P1");
        assertFalse(blocked.proceed());
        assertEquals("abort_mid_turn_no_restart", blocked.action());
    }
}
