package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0918PmOauthNotKnobAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("session-posture", AgenticMcp0918PmOauthNotKnobApp.KEY);
        assertEquals("agentic-mcp-0918-pm-oauth-not-knob", AgenticMcp0918PmOauthNotKnobApp.FOLDER);
        assertEquals("focus=admin-wall,shopping-pause=off", AgenticMcp0918PmOauthNotKnobApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0918PmOauthNotKnobApp.decide(null);
        assertEquals("focus=admin-wall,shopping-pause=off", d.value());
        assertEquals("share_session_posture", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0918PmOauthNotKnobApp.decide("focus=admin-wall,shopping-pause=off");
        assertTrue(d.proceed());
        assertEquals("share_session_posture", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0918PmOauthNotKnobApp.decide("focus=admin-wall,shopping-pause=on");
        assertFalse(blocked.proceed());
        assertEquals("incident_pause_active", blocked.action());
    }
}
