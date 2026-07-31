package io.kiponos.examples.mesh;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuadSdkLiveMeshAppLogicTest {
    @Test
    void hubKeyIsStable() {
        assertEquals("mode", QuadSdkLiveMeshApp.KEY);
        assertFalse(QuadSdkLiveMeshApp.KEY.isBlank());
    }

    @Test
    void defaultIsNonEmpty() {
        assertFalse(QuadSdkLiveMeshApp.DEFAULT.isBlank());
    }
}
