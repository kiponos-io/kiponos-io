package io.kiponos.examples.mesh;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FullstackIncidentKillAppLogicTest {
    @Test
    void hubKeyIsStable() {
        assertEquals("path-enabled", FullstackIncidentKillApp.KEY);
        assertFalse(FullstackIncidentKillApp.KEY.isBlank());
    }

    @Test
    void defaultIsNonEmpty() {
        assertFalse(FullstackIncidentKillApp.DEFAULT.isBlank());
    }
}
