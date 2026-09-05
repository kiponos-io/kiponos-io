package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev1028PmMirrorLiveAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("device-live", AgenticDev1028PmMirrorLiveApp.KEY);
        assertEquals("agentic-dev-1028-pm-mirror-live", AgenticDev1028PmMirrorLiveApp.FOLDER);
        assertEquals("yes", AgenticDev1028PmMirrorLiveApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev1028PmMirrorLiveApp.decide(null);
        assertEquals("yes", d.value());
        assertEquals("mirror_device_live", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev1028PmMirrorLiveApp.decide("yes");
        assertTrue(d.proceed());
        assertEquals("mirror_device_live", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev1028PmMirrorLiveApp.decide("no");
        assertFalse(blocked.proceed());
        assertEquals("route_other_mirror_device", blocked.action());
    }
}
