package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0923PmLazySchemaAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("schema-budget", AgenticMcp0923PmLazySchemaApp.KEY);
        assertEquals("agentic-mcp-0923-pm-lazy-schema", AgenticMcp0923PmLazySchemaApp.FOLDER);
        assertEquals("4000", AgenticMcp0923PmLazySchemaApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0923PmLazySchemaApp.decide(null);
        assertEquals("4000", d.value());
        assertEquals("schema_within_budget", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0923PmLazySchemaApp.decide("8000");
        assertTrue(d.proceed());
        assertEquals("schema_within_budget", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0923PmLazySchemaApp.decide("0");
        assertFalse(blocked.proceed());
        assertEquals("schema_dump_blocked", blocked.action());
    }
}
