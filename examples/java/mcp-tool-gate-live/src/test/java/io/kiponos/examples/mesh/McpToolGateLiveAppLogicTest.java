package io.kiponos.examples.mesh;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class McpToolGateLiveAppLogicTest {
    @Test
    void hubKeyIsStable() {
        assertEquals("tools-allow", McpToolGateLiveApp.KEY);
        assertFalse(McpToolGateLiveApp.KEY.isBlank());
    }

    @Test
    void defaultIsNonEmpty() {
        assertFalse(McpToolGateLiveApp.DEFAULT.isBlank());
    }
}
