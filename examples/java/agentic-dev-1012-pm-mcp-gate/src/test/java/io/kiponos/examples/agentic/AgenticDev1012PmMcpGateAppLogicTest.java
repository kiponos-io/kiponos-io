package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev1012PmMcpGateAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("tools-allow", AgenticDev1012PmMcpGateApp.KEY);
        assertEquals("agentic-dev-1012-pm-mcp-gate", AgenticDev1012PmMcpGateApp.FOLDER);
        assertEquals("search,read", AgenticDev1012PmMcpGateApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev1012PmMcpGateApp.decide(null);
        assertEquals("search,read", d.value());
        assertEquals("allow_listed_tools", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev1012PmMcpGateApp.decide("search,read");
        assertTrue(d.proceed());
        assertEquals("allow_listed_tools", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev1012PmMcpGateApp.decide("search,read,write");
        assertFalse(blocked.proceed());
        assertEquals("deny_write_no_mcp_restart", blocked.action());
    }
}
