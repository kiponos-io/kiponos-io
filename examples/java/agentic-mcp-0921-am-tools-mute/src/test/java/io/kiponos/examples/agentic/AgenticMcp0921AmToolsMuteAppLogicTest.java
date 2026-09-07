package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0921AmToolsMuteAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("tools-mute", AgenticMcp0921AmToolsMuteApp.KEY);
        assertEquals("agentic-mcp-0921-am-tools-mute", AgenticMcp0921AmToolsMuteApp.FOLDER);
        assertEquals("none", AgenticMcp0921AmToolsMuteApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0921AmToolsMuteApp.decide(null);
        assertEquals("none", d.value());
        assertEquals("tool_logs_live", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0921AmToolsMuteApp.decide("none");
        assertTrue(d.proceed());
        assertEquals("tool_logs_live", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0921AmToolsMuteApp.decide("ops-late-bags");
        assertFalse(blocked.proceed());
        assertEquals("tool_logs_muted", blocked.action());
    }
}
