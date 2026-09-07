package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0908AmSchemaBudgetAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("schema-budget", AgenticMcp0908AmSchemaBudgetApp.KEY);
        assertEquals("agentic-mcp-0908-am-schema-budget", AgenticMcp0908AmSchemaBudgetApp.FOLDER);
        assertEquals("4000", AgenticMcp0908AmSchemaBudgetApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0908AmSchemaBudgetApp.decide(null);
        assertEquals("4000", d.value());
        assertEquals("schema_within_budget", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0908AmSchemaBudgetApp.decide("8000");
        assertTrue(d.proceed());
        assertEquals("schema_within_budget", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0908AmSchemaBudgetApp.decide("0");
        assertFalse(blocked.proceed());
        assertEquals("schema_dump_blocked", blocked.action());
    }
}
