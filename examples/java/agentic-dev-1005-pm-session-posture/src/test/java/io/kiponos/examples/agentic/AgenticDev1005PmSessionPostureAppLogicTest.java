package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev1005PmSessionPostureAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("session-posture", AgenticDev1005PmSessionPostureApp.KEY);
        assertEquals("agentic-dev-1005-pm-session-posture", AgenticDev1005PmSessionPostureApp.FOLDER);
        assertEquals("focus=admin-wall,shopping-pause=off", AgenticDev1005PmSessionPostureApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev1005PmSessionPostureApp.decide(null);
        assertEquals("focus=admin-wall,shopping-pause=off", d.value());
        assertEquals("share_session_posture", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev1005PmSessionPostureApp.decide("focus=admin-wall,shopping-pause=off");
        assertTrue(d.proceed());
        assertEquals("share_session_posture", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev1005PmSessionPostureApp.decide("focus=admin-wall,shopping-pause=on");
        assertFalse(blocked.proceed());
        assertEquals("incident_pause_active", blocked.action());
    }
}
