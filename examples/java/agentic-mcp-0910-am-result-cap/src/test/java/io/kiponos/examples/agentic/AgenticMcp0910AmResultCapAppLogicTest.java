package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0910AmResultCapAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("result-cap", AgenticMcp0910AmResultCapApp.KEY);
        assertEquals("agentic-mcp-0910-am-result-cap", AgenticMcp0910AmResultCapApp.FOLDER);
        assertEquals("2000", AgenticMcp0910AmResultCapApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0910AmResultCapApp.decide(null);
        assertEquals("2000", d.value());
        assertEquals("result_projected", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0910AmResultCapApp.decide("8000");
        assertTrue(d.proceed());
        assertEquals("result_projected", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0910AmResultCapApp.decide("0");
        assertFalse(blocked.proceed());
        assertEquals("result_too_fat", blocked.action());
    }
}
