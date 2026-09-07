package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0924PmOpenai128AppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("tools-allow", AgenticMcp0924PmOpenai128App.KEY);
        assertEquals("agentic-mcp-0924-pm-openai-128", AgenticMcp0924PmOpenai128App.FOLDER);
        assertEquals("search,read", AgenticMcp0924PmOpenai128App.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0924PmOpenai128App.decide(null);
        assertEquals("search,read", d.value());
        assertEquals("allow_listed_tools", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0924PmOpenai128App.decide("search,read");
        assertTrue(d.proceed());
        assertEquals("allow_listed_tools", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0924PmOpenai128App.decide("search,read,write");
        assertFalse(blocked.proceed());
        assertEquals("deny_write_no_mcp_restart", blocked.action());
    }
}
