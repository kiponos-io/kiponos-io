package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0917PmCodeModeAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("code-mode", AgenticMcp0917PmCodeModeApp.KEY);
        assertEquals("agentic-mcp-0917-pm-code-mode", AgenticMcp0917PmCodeModeApp.FOLDER);
        assertEquals("off", AgenticMcp0917PmCodeModeApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0917PmCodeModeApp.decide(null);
        assertEquals("off", d.value());
        assertEquals("code_mode_on", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0917PmCodeModeApp.decide("yes");
        assertTrue(d.proceed());
        assertEquals("code_mode_on", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0917PmCodeModeApp.decide("no");
        assertFalse(blocked.proceed());
        assertEquals("schema_dump_mode", blocked.action());
    }
}
