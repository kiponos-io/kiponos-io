package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp1003PmClaudeToolSearchAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("tool-search", AgenticMcp1003PmClaudeToolSearchApp.KEY);
        assertEquals("agentic-mcp-1003-pm-claude-tool-search", AgenticMcp1003PmClaudeToolSearchApp.FOLDER);
        assertEquals("on", AgenticMcp1003PmClaudeToolSearchApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp1003PmClaudeToolSearchApp.decide(null);
        assertEquals("on", d.value());
        assertEquals("defer_schemas", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp1003PmClaudeToolSearchApp.decide("yes");
        assertTrue(d.proceed());
        assertEquals("defer_schemas", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp1003PmClaudeToolSearchApp.decide("no");
        assertFalse(blocked.proceed());
        assertEquals("dump_all_schemas", blocked.action());
    }
}
