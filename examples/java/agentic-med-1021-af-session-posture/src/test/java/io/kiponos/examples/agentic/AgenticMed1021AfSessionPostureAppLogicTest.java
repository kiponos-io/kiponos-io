package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMed1021AfSessionPostureAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("session-posture", AgenticMed1021AfSessionPostureApp.KEY);
        assertEquals("agentic-med-1021-af-session-posture", AgenticMed1021AfSessionPostureApp.FOLDER);
        assertEquals("focus=admin-wall,shopping-pause=off", AgenticMed1021AfSessionPostureApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMed1021AfSessionPostureApp.decide(null);
        assertEquals("focus=admin-wall,shopping-pause=off", d.value());
        assertEquals("share_session_posture", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMed1021AfSessionPostureApp.decide("focus=admin-wall,shopping-pause=off");
        assertTrue(d.proceed());
        assertEquals("share_session_posture", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMed1021AfSessionPostureApp.decide("focus=admin-wall,shopping-pause=on");
        assertFalse(blocked.proceed());
        assertEquals("incident_pause_active", blocked.action());
    }
}
