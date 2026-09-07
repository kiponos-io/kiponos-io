package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp1001AmBloatConfusionAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("schema-budget", AgenticMcp1001AmBloatConfusionApp.KEY);
        assertEquals("agentic-mcp-1001-am-bloat-confusion", AgenticMcp1001AmBloatConfusionApp.FOLDER);
        assertEquals("4000", AgenticMcp1001AmBloatConfusionApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp1001AmBloatConfusionApp.decide(null);
        assertEquals("4000", d.value());
        assertEquals("schema_within_budget", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp1001AmBloatConfusionApp.decide("8000");
        assertTrue(d.proceed());
        assertEquals("schema_within_budget", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp1001AmBloatConfusionApp.decide("0");
        assertFalse(blocked.proceed());
        assertEquals("schema_dump_blocked", blocked.action());
    }
}
