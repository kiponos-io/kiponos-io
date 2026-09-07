package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0919PmVectorNotPostureAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("shared-truth", AgenticMcp0919PmVectorNotPostureApp.KEY);
        assertEquals("agentic-mcp-0919-pm-vector-not-posture", AgenticMcp0919PmVectorNotPostureApp.FOLDER);
        assertEquals("live", AgenticMcp0919PmVectorNotPostureApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0919PmVectorNotPostureApp.decide(null);
        assertEquals("live", d.value());
        assertEquals("peers_share_live_hub", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0919PmVectorNotPostureApp.decide("live");
        assertTrue(d.proceed());
        assertEquals("peers_share_live_hub", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0919PmVectorNotPostureApp.decide("stale");
        assertFalse(blocked.proceed());
        assertEquals("refuse_stale_host_argv", blocked.action());
    }
}
