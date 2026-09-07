package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0929AmStdioLifetimeAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("mcp-drain", AgenticMcp0929AmStdioLifetimeApp.KEY);
        assertEquals("agentic-mcp-0929-am-stdio-lifetime", AgenticMcp0929AmStdioLifetimeApp.FOLDER);
        assertEquals("off", AgenticMcp0929AmStdioLifetimeApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0929AmStdioLifetimeApp.decide(null);
        assertEquals("off", d.value());
        assertEquals("children_live", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0929AmStdioLifetimeApp.decide("off");
        assertTrue(d.proceed());
        assertEquals("children_live", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0929AmStdioLifetimeApp.decide("on");
        assertFalse(blocked.proceed());
        assertEquals("drain_mcp_children", blocked.action());
    }
}
