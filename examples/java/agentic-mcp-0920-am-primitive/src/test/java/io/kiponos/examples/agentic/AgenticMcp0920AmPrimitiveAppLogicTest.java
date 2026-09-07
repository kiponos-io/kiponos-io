package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0920AmPrimitiveAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("shared-truth", AgenticMcp0920AmPrimitiveApp.KEY);
        assertEquals("agentic-mcp-0920-am-primitive", AgenticMcp0920AmPrimitiveApp.FOLDER);
        assertEquals("live", AgenticMcp0920AmPrimitiveApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0920AmPrimitiveApp.decide(null);
        assertEquals("live", d.value());
        assertEquals("peers_share_live_hub", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0920AmPrimitiveApp.decide("live");
        assertTrue(d.proceed());
        assertEquals("peers_share_live_hub", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0920AmPrimitiveApp.decide("stale");
        assertFalse(blocked.proceed());
        assertEquals("refuse_stale_host_argv", blocked.action());
    }
}
