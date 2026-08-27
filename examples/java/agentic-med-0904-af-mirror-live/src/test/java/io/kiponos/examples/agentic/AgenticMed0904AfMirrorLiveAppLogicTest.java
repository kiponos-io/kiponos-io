package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMed0904AfMirrorLiveAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("device-live", AgenticMed0904AfMirrorLiveApp.KEY);
        assertEquals("agentic-med-0904-af-mirror-live", AgenticMed0904AfMirrorLiveApp.FOLDER);
        assertEquals("yes", AgenticMed0904AfMirrorLiveApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMed0904AfMirrorLiveApp.decide(null);
        assertEquals("yes", d.value());
        assertEquals("mirror_device_live", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMed0904AfMirrorLiveApp.decide("yes");
        assertTrue(d.proceed());
        assertEquals("mirror_device_live", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMed0904AfMirrorLiveApp.decide("no");
        assertFalse(blocked.proceed());
        assertEquals("route_other_mirror_device", blocked.action());
    }
}
