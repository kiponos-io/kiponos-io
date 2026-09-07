package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp1005PmSandboxNotPostureAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("execute-gate", AgenticMcp1005PmSandboxNotPostureApp.KEY);
        assertEquals("agentic-mcp-1005-pm-sandbox-not-posture", AgenticMcp1005PmSandboxNotPostureApp.FOLDER);
        assertEquals("plan", AgenticMcp1005PmSandboxNotPostureApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp1005PmSandboxNotPostureApp.decide(null);
        assertEquals("plan", d.value());
        assertEquals("execute_allowed", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp1005PmSandboxNotPostureApp.decide("checkout");
        assertTrue(d.proceed());
        assertEquals("execute_allowed", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp1005PmSandboxNotPostureApp.decide("idle");
        assertFalse(blocked.proceed());
        assertEquals("stay_in_plan", blocked.action());
    }
}
