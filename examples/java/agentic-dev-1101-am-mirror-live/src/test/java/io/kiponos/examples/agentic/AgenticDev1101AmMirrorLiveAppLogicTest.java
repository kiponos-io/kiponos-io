package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev1101AmMirrorLiveAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("device-live", AgenticDev1101AmMirrorLiveApp.KEY);
        assertEquals("agentic-dev-1101-am-mirror-live", AgenticDev1101AmMirrorLiveApp.FOLDER);
        assertEquals("yes", AgenticDev1101AmMirrorLiveApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev1101AmMirrorLiveApp.decide(null);
        assertEquals("yes", d.value());
        assertEquals("mirror_device_live", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev1101AmMirrorLiveApp.decide("yes");
        assertTrue(d.proceed());
        assertEquals("mirror_device_live", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev1101AmMirrorLiveApp.decide("no");
        assertFalse(blocked.proceed());
        assertEquals("route_other_mirror_device", blocked.action());
    }
}
