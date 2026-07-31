package io.kiponos.examples.mesh;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WarRoomSharedTruthAppLogicTest {
    @Test
    void hubKeyIsStable() {
        assertEquals("headline", WarRoomSharedTruthApp.KEY);
        assertFalse(WarRoomSharedTruthApp.KEY.isBlank());
    }

    @Test
    void defaultIsNonEmpty() {
        assertFalse(WarRoomSharedTruthApp.DEFAULT.isBlank());
    }
}
