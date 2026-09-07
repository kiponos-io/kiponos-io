package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0914PmSubagentInheritAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("inherit-posture", AgenticMcp0914PmSubagentInheritApp.KEY);
        assertEquals("agentic-mcp-0914-pm-subagent-inherit", AgenticMcp0914PmSubagentInheritApp.FOLDER);
        assertEquals("on", AgenticMcp0914PmSubagentInheritApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0914PmSubagentInheritApp.decide(null);
        assertEquals("on", d.value());
        assertEquals("child_reads_hub", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0914PmSubagentInheritApp.decide("yes");
        assertTrue(d.proceed());
        assertEquals("child_reads_hub", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0914PmSubagentInheritApp.decide("no");
        assertFalse(blocked.proceed());
        assertEquals("child_frozen_argv", blocked.action());
    }
}
