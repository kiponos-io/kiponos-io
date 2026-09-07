package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0914AmTwoHostsAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("session-posture", AgenticMcp0914AmTwoHostsApp.KEY);
        assertEquals("agentic-mcp-0914-am-two-hosts", AgenticMcp0914AmTwoHostsApp.FOLDER);
        assertEquals("focus=admin-wall,shopping-pause=off", AgenticMcp0914AmTwoHostsApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0914AmTwoHostsApp.decide(null);
        assertEquals("focus=admin-wall,shopping-pause=off", d.value());
        assertEquals("share_session_posture", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0914AmTwoHostsApp.decide("focus=admin-wall,shopping-pause=off");
        assertTrue(d.proceed());
        assertEquals("share_session_posture", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0914AmTwoHostsApp.decide("focus=admin-wall,shopping-pause=on");
        assertFalse(blocked.proceed());
        assertEquals("incident_pause_active", blocked.action());
    }
}
