package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0909AmContextRotAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("compact-mode", AgenticMcp0909AmContextRotApp.KEY);
        assertEquals("agentic-mcp-0909-am-context-rot", AgenticMcp0909AmContextRotApp.FOLDER);
        assertEquals("off", AgenticMcp0909AmContextRotApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0909AmContextRotApp.decide(null);
        assertEquals("off", d.value());
        assertEquals("compact_on", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0909AmContextRotApp.decide("yes");
        assertTrue(d.proceed());
        assertEquals("compact_on", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0909AmContextRotApp.decide("no");
        assertFalse(blocked.proceed());
        assertEquals("full_dump", blocked.action());
    }
}
