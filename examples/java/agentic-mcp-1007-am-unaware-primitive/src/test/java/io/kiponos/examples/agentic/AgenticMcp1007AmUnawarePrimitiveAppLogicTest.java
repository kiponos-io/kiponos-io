package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp1007AmUnawarePrimitiveAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("shared-truth", AgenticMcp1007AmUnawarePrimitiveApp.KEY);
        assertEquals("agentic-mcp-1007-am-unaware-primitive", AgenticMcp1007AmUnawarePrimitiveApp.FOLDER);
        assertEquals("live", AgenticMcp1007AmUnawarePrimitiveApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp1007AmUnawarePrimitiveApp.decide(null);
        assertEquals("live", d.value());
        assertEquals("peers_share_live_hub", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp1007AmUnawarePrimitiveApp.decide("live");
        assertTrue(d.proceed());
        assertEquals("peers_share_live_hub", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp1007AmUnawarePrimitiveApp.decide("stale");
        assertFalse(blocked.proceed());
        assertEquals("refuse_stale_host_argv", blocked.action());
    }
}
