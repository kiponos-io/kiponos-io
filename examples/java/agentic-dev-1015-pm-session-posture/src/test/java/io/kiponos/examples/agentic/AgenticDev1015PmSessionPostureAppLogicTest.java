package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev1015PmSessionPostureAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("session-posture", AgenticDev1015PmSessionPostureApp.KEY);
        assertEquals("agentic-dev-1015-pm-session-posture", AgenticDev1015PmSessionPostureApp.FOLDER);
        assertEquals("focus=admin-wall,shopping-pause=off", AgenticDev1015PmSessionPostureApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev1015PmSessionPostureApp.decide(null);
        assertEquals("focus=admin-wall,shopping-pause=off", d.value());
        assertEquals("share_session_posture", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev1015PmSessionPostureApp.decide("focus=admin-wall,shopping-pause=off");
        assertTrue(d.proceed());
        assertEquals("share_session_posture", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev1015PmSessionPostureApp.decide("focus=admin-wall,shopping-pause=on");
        assertFalse(blocked.proceed());
        assertEquals("incident_pause_active", blocked.action());
    }
}
