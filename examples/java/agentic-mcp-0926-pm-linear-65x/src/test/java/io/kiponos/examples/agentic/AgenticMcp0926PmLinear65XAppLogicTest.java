package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0926PmLinear65XAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("result-cap", AgenticMcp0926PmLinear65XApp.KEY);
        assertEquals("agentic-mcp-0926-pm-linear-65x", AgenticMcp0926PmLinear65XApp.FOLDER);
        assertEquals("2000", AgenticMcp0926PmLinear65XApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0926PmLinear65XApp.decide(null);
        assertEquals("2000", d.value());
        assertEquals("result_projected", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0926PmLinear65XApp.decide("8000");
        assertTrue(d.proceed());
        assertEquals("result_projected", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0926PmLinear65XApp.decide("0");
        assertFalse(blocked.proceed());
        assertEquals("result_too_fat", blocked.action());
    }
}
