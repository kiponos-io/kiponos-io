package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0922AmIncidentWritesAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("incident-pause", AgenticMcp0922AmIncidentWritesApp.KEY);
        assertEquals("agentic-mcp-0922-am-incident-writes", AgenticMcp0922AmIncidentWritesApp.FOLDER);
        assertEquals("off", AgenticMcp0922AmIncidentWritesApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0922AmIncidentWritesApp.decide(null);
        assertEquals("off", d.value());
        assertEquals("shopping_path_live", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0922AmIncidentWritesApp.decide("off");
        assertTrue(d.proceed());
        assertEquals("shopping_path_live", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0922AmIncidentWritesApp.decide("on");
        assertFalse(blocked.proceed());
        assertEquals("freeze_shopping_writes", blocked.action());
    }
}
