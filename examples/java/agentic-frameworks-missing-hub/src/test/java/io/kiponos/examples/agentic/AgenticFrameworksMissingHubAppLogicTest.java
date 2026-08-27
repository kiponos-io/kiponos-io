package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticFrameworksMissingHubAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("shared-truth", AgenticFrameworksMissingHubApp.KEY);
        assertEquals("agentic-frameworks-missing-hub", AgenticFrameworksMissingHubApp.FOLDER);
        assertEquals("live", AgenticFrameworksMissingHubApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticFrameworksMissingHubApp.decide(null);
        assertEquals("live", d.value());
        assertEquals("peers_share_live_hub", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticFrameworksMissingHubApp.decide("live");
        assertTrue(d.proceed());
        assertEquals("peers_share_live_hub", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticFrameworksMissingHubApp.decide("stale");
        assertFalse(blocked.proceed());
        assertEquals("refuse_stale_host_argv", blocked.action());
    }
}
