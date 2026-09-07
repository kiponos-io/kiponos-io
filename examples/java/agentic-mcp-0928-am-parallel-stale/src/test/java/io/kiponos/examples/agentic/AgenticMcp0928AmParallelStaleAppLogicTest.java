package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0928AmParallelStaleAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("inherit-posture", AgenticMcp0928AmParallelStaleApp.KEY);
        assertEquals("agentic-mcp-0928-am-parallel-stale", AgenticMcp0928AmParallelStaleApp.FOLDER);
        assertEquals("on", AgenticMcp0928AmParallelStaleApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0928AmParallelStaleApp.decide(null);
        assertEquals("on", d.value());
        assertEquals("child_reads_hub", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0928AmParallelStaleApp.decide("yes");
        assertTrue(d.proceed());
        assertEquals("child_reads_hub", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0928AmParallelStaleApp.decide("no");
        assertFalse(blocked.proceed());
        assertEquals("child_frozen_argv", blocked.action());
    }
}
