package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0915AmExecuteGateAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("execute-gate", AgenticMcp0915AmExecuteGateApp.KEY);
        assertEquals("agentic-mcp-0915-am-execute-gate", AgenticMcp0915AmExecuteGateApp.FOLDER);
        assertEquals("plan", AgenticMcp0915AmExecuteGateApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0915AmExecuteGateApp.decide(null);
        assertEquals("plan", d.value());
        assertEquals("execute_allowed", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0915AmExecuteGateApp.decide("checkout");
        assertTrue(d.proceed());
        assertEquals("execute_allowed", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0915AmExecuteGateApp.decide("idle");
        assertFalse(blocked.proceed());
        assertEquals("stay_in_plan", blocked.action());
    }
}
