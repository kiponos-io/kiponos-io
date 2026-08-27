package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMed0901AfSessionPostureAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("session-posture", AgenticMed0901AfSessionPostureApp.KEY);
        assertEquals("agentic-med-0901-af-session-posture", AgenticMed0901AfSessionPostureApp.FOLDER);
        assertEquals("focus=admin-wall,shopping-pause=off", AgenticMed0901AfSessionPostureApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMed0901AfSessionPostureApp.decide(null);
        assertEquals("focus=admin-wall,shopping-pause=off", d.value());
        assertEquals("share_session_posture", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMed0901AfSessionPostureApp.decide("focus=admin-wall,shopping-pause=off");
        assertTrue(d.proceed());
        assertEquals("share_session_posture", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMed0901AfSessionPostureApp.decide("focus=admin-wall,shopping-pause=on");
        assertFalse(blocked.proceed());
        assertEquals("incident_pause_active", blocked.action());
    }
}
