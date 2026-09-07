package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0910PmProgressiveAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("tool-search", AgenticMcp0910PmProgressiveApp.KEY);
        assertEquals("agentic-mcp-0910-pm-progressive", AgenticMcp0910PmProgressiveApp.FOLDER);
        assertEquals("on", AgenticMcp0910PmProgressiveApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0910PmProgressiveApp.decide(null);
        assertEquals("on", d.value());
        assertEquals("defer_schemas", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0910PmProgressiveApp.decide("yes");
        assertTrue(d.proceed());
        assertEquals("defer_schemas", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0910PmProgressiveApp.decide("no");
        assertFalse(blocked.proceed());
        assertEquals("dump_all_schemas", blocked.action());
    }
}
