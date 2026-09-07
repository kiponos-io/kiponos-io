package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0923AmWarRoomAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("session-posture", AgenticMcp0923AmWarRoomApp.KEY);
        assertEquals("agentic-mcp-0923-am-war-room", AgenticMcp0923AmWarRoomApp.FOLDER);
        assertEquals("focus=admin-wall,shopping-pause=off", AgenticMcp0923AmWarRoomApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0923AmWarRoomApp.decide(null);
        assertEquals("focus=admin-wall,shopping-pause=off", d.value());
        assertEquals("share_session_posture", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0923AmWarRoomApp.decide("focus=admin-wall,shopping-pause=off");
        assertTrue(d.proceed());
        assertEquals("share_session_posture", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0923AmWarRoomApp.decide("focus=admin-wall,shopping-pause=on");
        assertFalse(blocked.proceed());
        assertEquals("incident_pause_active", blocked.action());
    }
}
