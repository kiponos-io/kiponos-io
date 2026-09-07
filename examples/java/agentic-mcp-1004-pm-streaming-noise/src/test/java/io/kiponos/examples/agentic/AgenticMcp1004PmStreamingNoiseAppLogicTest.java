package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp1004PmStreamingNoiseAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("result-cap", AgenticMcp1004PmStreamingNoiseApp.KEY);
        assertEquals("agentic-mcp-1004-pm-streaming-noise", AgenticMcp1004PmStreamingNoiseApp.FOLDER);
        assertEquals("2000", AgenticMcp1004PmStreamingNoiseApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp1004PmStreamingNoiseApp.decide(null);
        assertEquals("2000", d.value());
        assertEquals("result_projected", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp1004PmStreamingNoiseApp.decide("8000");
        assertTrue(d.proceed());
        assertEquals("result_projected", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp1004PmStreamingNoiseApp.decide("0");
        assertFalse(blocked.proceed());
        assertEquals("result_too_fat", blocked.action());
    }
}
