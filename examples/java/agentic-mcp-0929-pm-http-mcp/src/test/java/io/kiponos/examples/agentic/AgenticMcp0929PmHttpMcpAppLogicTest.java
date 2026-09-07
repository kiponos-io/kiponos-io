package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0929PmHttpMcpAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("tools-allow", AgenticMcp0929PmHttpMcpApp.KEY);
        assertEquals("agentic-mcp-0929-pm-http-mcp", AgenticMcp0929PmHttpMcpApp.FOLDER);
        assertEquals("search,read", AgenticMcp0929PmHttpMcpApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0929PmHttpMcpApp.decide(null);
        assertEquals("search,read", d.value());
        assertEquals("allow_listed_tools", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0929PmHttpMcpApp.decide("search,read");
        assertTrue(d.proceed());
        assertEquals("allow_listed_tools", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0929PmHttpMcpApp.decide("search,read,write");
        assertFalse(blocked.proceed());
        assertEquals("deny_write_no_mcp_restart", blocked.action());
    }
}
