package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev1101PmMcpGateAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("tools-allow", AgenticDev1101PmMcpGateApp.KEY);
        assertEquals("agentic-dev-1101-pm-mcp-gate", AgenticDev1101PmMcpGateApp.FOLDER);
        assertEquals("search,read", AgenticDev1101PmMcpGateApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev1101PmMcpGateApp.decide(null);
        assertEquals("search,read", d.value());
        assertEquals("allow_listed_tools", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev1101PmMcpGateApp.decide("search,read");
        assertTrue(d.proceed());
        assertEquals("allow_listed_tools", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev1101PmMcpGateApp.decide("search,read,write");
        assertFalse(blocked.proceed());
        assertEquals("deny_write_no_mcp_restart", blocked.action());
    }
}
