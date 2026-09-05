package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev1029AmSessionPostureAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("session-posture", AgenticDev1029AmSessionPostureApp.KEY);
        assertEquals("agentic-dev-1029-am-session-posture", AgenticDev1029AmSessionPostureApp.FOLDER);
        assertEquals("focus=admin-wall,shopping-pause=off", AgenticDev1029AmSessionPostureApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev1029AmSessionPostureApp.decide(null);
        assertEquals("focus=admin-wall,shopping-pause=off", d.value());
        assertEquals("share_session_posture", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev1029AmSessionPostureApp.decide("focus=admin-wall,shopping-pause=off");
        assertTrue(d.proceed());
        assertEquals("share_session_posture", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev1029AmSessionPostureApp.decide("focus=admin-wall,shopping-pause=on");
        assertFalse(blocked.proceed());
        assertEquals("incident_pause_active", blocked.action());
    }
}
