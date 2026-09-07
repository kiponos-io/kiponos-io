package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0928PmHostCeremonyAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("tools-allow", AgenticMcp0928PmHostCeremonyApp.KEY);
        assertEquals("agentic-mcp-0928-pm-host-ceremony", AgenticMcp0928PmHostCeremonyApp.FOLDER);
        assertEquals("search,read", AgenticMcp0928PmHostCeremonyApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0928PmHostCeremonyApp.decide(null);
        assertEquals("search,read", d.value());
        assertEquals("allow_listed_tools", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0928PmHostCeremonyApp.decide("search,read");
        assertTrue(d.proceed());
        assertEquals("allow_listed_tools", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0928PmHostCeremonyApp.decide("search,read,write");
        assertFalse(blocked.proceed());
        assertEquals("deny_write_no_mcp_restart", blocked.action());
    }
}
