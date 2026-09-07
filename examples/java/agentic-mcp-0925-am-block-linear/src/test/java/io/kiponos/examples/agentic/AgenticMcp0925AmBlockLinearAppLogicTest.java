package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0925AmBlockLinearAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("result-cap", AgenticMcp0925AmBlockLinearApp.KEY);
        assertEquals("agentic-mcp-0925-am-block-linear", AgenticMcp0925AmBlockLinearApp.FOLDER);
        assertEquals("2000", AgenticMcp0925AmBlockLinearApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0925AmBlockLinearApp.decide(null);
        assertEquals("2000", d.value());
        assertEquals("result_projected", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0925AmBlockLinearApp.decide("8000");
        assertTrue(d.proceed());
        assertEquals("result_projected", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0925AmBlockLinearApp.decide("0");
        assertFalse(blocked.proceed());
        assertEquals("result_too_fat", blocked.action());
    }
}
