package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev1006AmMcpGateAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("tools-allow", AgenticDev1006AmMcpGateApp.KEY);
        assertEquals("agentic-dev-1006-am-mcp-gate", AgenticDev1006AmMcpGateApp.FOLDER);
        assertEquals("search,read", AgenticDev1006AmMcpGateApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev1006AmMcpGateApp.decide(null);
        assertEquals("search,read", d.value());
        assertEquals("allow_listed_tools", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev1006AmMcpGateApp.decide("search,read");
        assertTrue(d.proceed());
        assertEquals("allow_listed_tools", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev1006AmMcpGateApp.decide("search,read,write");
        assertFalse(blocked.proceed());
        assertEquals("deny_write_no_mcp_restart", blocked.action());
    }
}
