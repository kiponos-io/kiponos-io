package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMed0927MiMirrorLiveAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("device-live", AgenticMed0927MiMirrorLiveApp.KEY);
        assertEquals("agentic-med-0927-mi-mirror-live", AgenticMed0927MiMirrorLiveApp.FOLDER);
        assertEquals("yes", AgenticMed0927MiMirrorLiveApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMed0927MiMirrorLiveApp.decide(null);
        assertEquals("yes", d.value());
        assertEquals("mirror_device_live", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMed0927MiMirrorLiveApp.decide("yes");
        assertTrue(d.proceed());
        assertEquals("mirror_device_live", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMed0927MiMirrorLiveApp.decide("no");
        assertFalse(blocked.proceed());
        assertEquals("route_other_mirror_device", blocked.action());
    }
}
