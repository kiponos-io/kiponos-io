package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0912PmSkillVsMcpAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("shared-truth", AgenticMcp0912PmSkillVsMcpApp.KEY);
        assertEquals("agentic-mcp-0912-pm-skill-vs-mcp", AgenticMcp0912PmSkillVsMcpApp.FOLDER);
        assertEquals("live", AgenticMcp0912PmSkillVsMcpApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0912PmSkillVsMcpApp.decide(null);
        assertEquals("live", d.value());
        assertEquals("peers_share_live_hub", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0912PmSkillVsMcpApp.decide("live");
        assertTrue(d.proceed());
        assertEquals("peers_share_live_hub", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0912PmSkillVsMcpApp.decide("stale");
        assertFalse(blocked.proceed());
        assertEquals("refuse_stale_host_argv", blocked.action());
    }
}
