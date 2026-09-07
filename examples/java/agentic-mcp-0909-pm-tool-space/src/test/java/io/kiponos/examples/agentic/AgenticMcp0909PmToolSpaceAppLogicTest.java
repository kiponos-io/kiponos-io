package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0909PmToolSpaceAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("tools-allow", AgenticMcp0909PmToolSpaceApp.KEY);
        assertEquals("agentic-mcp-0909-pm-tool-space", AgenticMcp0909PmToolSpaceApp.FOLDER);
        assertEquals("search,read", AgenticMcp0909PmToolSpaceApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0909PmToolSpaceApp.decide(null);
        assertEquals("search,read", d.value());
        assertEquals("allow_listed_tools", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0909PmToolSpaceApp.decide("search,read");
        assertTrue(d.proceed());
        assertEquals("allow_listed_tools", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0909PmToolSpaceApp.decide("search,read,write");
        assertFalse(blocked.proceed());
        assertEquals("deny_write_no_mcp_restart", blocked.action());
    }
}
