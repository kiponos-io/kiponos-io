package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev0902PmMcpGateAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("tools-allow", AgenticDev0902PmMcpGateApp.KEY);
        assertEquals("agentic-dev-0902-pm-mcp-gate", AgenticDev0902PmMcpGateApp.FOLDER);
        assertEquals("search,read", AgenticDev0902PmMcpGateApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev0902PmMcpGateApp.decide(null);
        assertEquals("search,read", d.value());
        assertEquals("allow_listed_tools", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev0902PmMcpGateApp.decide("search,read");
        assertTrue(d.proceed());
        assertEquals("allow_listed_tools", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev0902PmMcpGateApp.decide("search,read,write");
        assertFalse(blocked.proceed());
        assertEquals("deny_write_no_mcp_restart", blocked.action());
    }
}
