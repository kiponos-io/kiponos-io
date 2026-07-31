package io.kiponos.examples.mesh;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ObsTraceSampleMeshAppLogicTest {
    @Test
    void hubKeyIsStable() {
        assertEquals("sample-percent", ObsTraceSampleMeshApp.KEY);
        assertFalse(ObsTraceSampleMeshApp.KEY.isBlank());
    }

    @Test
    void defaultIsNonEmpty() {
        assertFalse(ObsTraceSampleMeshApp.DEFAULT.isBlank());
    }
}
