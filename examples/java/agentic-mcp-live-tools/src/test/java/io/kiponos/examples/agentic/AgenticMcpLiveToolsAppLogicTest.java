package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcpLiveToolsAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("tools-allow", AgenticMcpLiveToolsApp.KEY);
        assertEquals("agentic-mcp-live-tools", AgenticMcpLiveToolsApp.FOLDER);
        assertEquals("search,read", AgenticMcpLiveToolsApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcpLiveToolsApp.decide(null);
        assertEquals("search,read", d.value());
        assertEquals("allow_listed_tools", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcpLiveToolsApp.decide("search,read");
        assertTrue(d.proceed());
        assertEquals("allow_listed_tools", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcpLiveToolsApp.decide("search,read,write");
        assertFalse(blocked.proceed());
        assertEquals("deny_write_no_mcp_restart", blocked.action());
    }
}
