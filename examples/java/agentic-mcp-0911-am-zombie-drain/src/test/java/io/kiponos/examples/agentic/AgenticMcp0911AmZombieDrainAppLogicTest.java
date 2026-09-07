package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0911AmZombieDrainAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("mcp-drain", AgenticMcp0911AmZombieDrainApp.KEY);
        assertEquals("agentic-mcp-0911-am-zombie-drain", AgenticMcp0911AmZombieDrainApp.FOLDER);
        assertEquals("off", AgenticMcp0911AmZombieDrainApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0911AmZombieDrainApp.decide(null);
        assertEquals("off", d.value());
        assertEquals("children_live", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0911AmZombieDrainApp.decide("off");
        assertTrue(d.proceed());
        assertEquals("children_live", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0911AmZombieDrainApp.decide("on");
        assertFalse(blocked.proceed());
        assertEquals("drain_mcp_children", blocked.action());
    }
}
