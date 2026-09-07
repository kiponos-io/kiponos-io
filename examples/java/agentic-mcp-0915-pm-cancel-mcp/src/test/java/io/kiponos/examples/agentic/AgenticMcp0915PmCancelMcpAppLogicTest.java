package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0915PmCancelMcpAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("cancel-token", AgenticMcp0915PmCancelMcpApp.KEY);
        assertEquals("agentic-mcp-0915-pm-cancel-mcp", AgenticMcp0915PmCancelMcpApp.FOLDER);
        assertEquals("off", AgenticMcp0915PmCancelMcpApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0915PmCancelMcpApp.decide(null);
        assertEquals("off", d.value());
        assertEquals("mcp_task_live", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0915PmCancelMcpApp.decide("off");
        assertTrue(d.proceed());
        assertEquals("mcp_task_live", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0915PmCancelMcpApp.decide("on");
        assertFalse(blocked.proceed());
        assertEquals("cancel_mcp_task", blocked.action());
    }
}
