package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0919AmFlagsNotSessionAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("tools-allow", AgenticMcp0919AmFlagsNotSessionApp.KEY);
        assertEquals("agentic-mcp-0919-am-flags-not-session", AgenticMcp0919AmFlagsNotSessionApp.FOLDER);
        assertEquals("search,read", AgenticMcp0919AmFlagsNotSessionApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0919AmFlagsNotSessionApp.decide(null);
        assertEquals("search,read", d.value());
        assertEquals("allow_listed_tools", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0919AmFlagsNotSessionApp.decide("search,read");
        assertTrue(d.proceed());
        assertEquals("allow_listed_tools", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0919AmFlagsNotSessionApp.decide("search,read,write");
        assertFalse(blocked.proceed());
        assertEquals("deny_write_no_mcp_restart", blocked.action());
    }
}
