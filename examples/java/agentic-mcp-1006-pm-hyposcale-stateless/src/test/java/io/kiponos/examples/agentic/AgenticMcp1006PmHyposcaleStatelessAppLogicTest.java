package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp1006PmHyposcaleStatelessAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("shared-truth", AgenticMcp1006PmHyposcaleStatelessApp.KEY);
        assertEquals("agentic-mcp-1006-pm-hyposcale-stateless", AgenticMcp1006PmHyposcaleStatelessApp.FOLDER);
        assertEquals("live", AgenticMcp1006PmHyposcaleStatelessApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp1006PmHyposcaleStatelessApp.decide(null);
        assertEquals("live", d.value());
        assertEquals("peers_share_live_hub", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp1006PmHyposcaleStatelessApp.decide("live");
        assertTrue(d.proceed());
        assertEquals("peers_share_live_hub", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp1006PmHyposcaleStatelessApp.decide("stale");
        assertFalse(blocked.proceed());
        assertEquals("refuse_stale_host_argv", blocked.action());
    }
}
