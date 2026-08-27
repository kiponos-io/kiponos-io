package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMed1008AfMcpGateAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("tools-allow", AgenticMed1008AfMcpGateApp.KEY);
        assertEquals("agentic-med-1008-af-mcp-gate", AgenticMed1008AfMcpGateApp.FOLDER);
        assertEquals("search,read", AgenticMed1008AfMcpGateApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMed1008AfMcpGateApp.decide(null);
        assertEquals("search,read", d.value());
        assertEquals("allow_listed_tools", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMed1008AfMcpGateApp.decide("search,read");
        assertTrue(d.proceed());
        assertEquals("allow_listed_tools", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMed1008AfMcpGateApp.decide("search,read,write");
        assertFalse(blocked.proceed());
        assertEquals("deny_write_no_mcp_restart", blocked.action());
    }
}
