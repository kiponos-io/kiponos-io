package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticCursorSessionSharedStateAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("session-posture", AgenticCursorSessionSharedStateApp.KEY);
        assertEquals("agentic-cursor-session-shared-state", AgenticCursorSessionSharedStateApp.FOLDER);
        assertEquals("focus=admin-wall,shopping-pause=off", AgenticCursorSessionSharedStateApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticCursorSessionSharedStateApp.decide(null);
        assertEquals("focus=admin-wall,shopping-pause=off", d.value());
        assertEquals("share_session_posture", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticCursorSessionSharedStateApp.decide("focus=admin-wall,shopping-pause=off");
        assertTrue(d.proceed());
        assertEquals("share_session_posture", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticCursorSessionSharedStateApp.decide("focus=admin-wall,shopping-pause=on");
        assertFalse(blocked.proceed());
        assertEquals("incident_pause_active", blocked.action());
    }
}
