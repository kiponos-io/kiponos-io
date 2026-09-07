package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0920PmCanaryToolsAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("canary-percent", AgenticMcp0920PmCanaryToolsApp.KEY);
        assertEquals("agentic-mcp-0920-pm-canary-tools", AgenticMcp0920PmCanaryToolsApp.FOLDER);
        assertEquals("0", AgenticMcp0920PmCanaryToolsApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0920PmCanaryToolsApp.decide(null);
        assertEquals("0", d.value());
        assertEquals("canary_tools_live", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0920PmCanaryToolsApp.decide("8000");
        assertTrue(d.proceed());
        assertEquals("canary_tools_live", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0920PmCanaryToolsApp.decide("0");
        assertFalse(blocked.proceed());
        assertEquals("canary_tools_off", blocked.action());
    }
}
