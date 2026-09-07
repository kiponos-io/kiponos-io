package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0918AmClientBlameAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("tool-search", AgenticMcp0918AmClientBlameApp.KEY);
        assertEquals("agentic-mcp-0918-am-client-blame", AgenticMcp0918AmClientBlameApp.FOLDER);
        assertEquals("on", AgenticMcp0918AmClientBlameApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0918AmClientBlameApp.decide(null);
        assertEquals("on", d.value());
        assertEquals("defer_schemas", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0918AmClientBlameApp.decide("yes");
        assertTrue(d.proceed());
        assertEquals("defer_schemas", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0918AmClientBlameApp.decide("no");
        assertFalse(blocked.proceed());
        assertEquals("dump_all_schemas", blocked.action());
    }
}
