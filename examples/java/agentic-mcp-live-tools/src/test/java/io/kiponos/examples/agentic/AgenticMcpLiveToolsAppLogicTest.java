package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcpLiveToolsAppLogicTest {
    @Test
    void hubKeyIsStable() {
        assertEquals("tools-allow", AgenticMcpLiveToolsApp.KEY);
        assertEquals("agentic-mcp-live-tools", AgenticMcpLiveToolsApp.FOLDER);
        assertFalse(AgenticMcpLiveToolsApp.KEY.isBlank());
    }

    @Test
    void defaultIsNonEmpty() {
        assertFalse(AgenticMcpLiveToolsApp.DEFAULT.isBlank());
    }
}
