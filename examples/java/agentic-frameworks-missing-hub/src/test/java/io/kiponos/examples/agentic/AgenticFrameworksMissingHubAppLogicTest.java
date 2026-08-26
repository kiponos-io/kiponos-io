package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticFrameworksMissingHubAppLogicTest {
    @Test
    void hubKeyIsStable() {
        assertEquals("shared-truth", AgenticFrameworksMissingHubApp.KEY);
        assertEquals("agentic-frameworks-missing-hub", AgenticFrameworksMissingHubApp.FOLDER);
        assertFalse(AgenticFrameworksMissingHubApp.KEY.isBlank());
    }

    @Test
    void defaultIsNonEmpty() {
        assertFalse(AgenticFrameworksMissingHubApp.DEFAULT.isBlank());
    }
}
