package io.kiponos.examples.mesh;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClientServerMirrorLiveAppLogicTest {
    @Test
    void hubKeyIsStable() {
        assertEquals("status", ClientServerMirrorLiveApp.KEY);
        assertFalse(ClientServerMirrorLiveApp.KEY.isBlank());
    }

    @Test
    void defaultIsNonEmpty() {
        assertFalse(ClientServerMirrorLiveApp.DEFAULT.isBlank());
    }
}
