package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0917AmDoomLoopAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("retry-max", AgenticMcp0917AmDoomLoopApp.KEY);
        assertEquals("agentic-mcp-0917-am-doom-loop", AgenticMcp0917AmDoomLoopApp.FOLDER);
        assertEquals("2", AgenticMcp0917AmDoomLoopApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0917AmDoomLoopApp.decide(null);
        assertEquals("2", d.value());
        assertEquals("retry_within_cap", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0917AmDoomLoopApp.decide("8000");
        assertTrue(d.proceed());
        assertEquals("retry_within_cap", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0917AmDoomLoopApp.decide("0");
        assertFalse(blocked.proceed());
        assertEquals("doom_loop_stopped", blocked.action());
    }
}
