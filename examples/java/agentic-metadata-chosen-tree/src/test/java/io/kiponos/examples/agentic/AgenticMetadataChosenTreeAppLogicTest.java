package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMetadataChosenTreeAppLogicTest {
    @Test
    void hubKeyIsStable() {
        assertEquals("owner-agent", AgenticMetadataChosenTreeApp.KEY);
        assertEquals("agentic-metadata-chosen-tree", AgenticMetadataChosenTreeApp.FOLDER);
        assertFalse(AgenticMetadataChosenTreeApp.KEY.isBlank());
    }

    @Test
    void defaultIsNonEmpty() {
        assertFalse(AgenticMetadataChosenTreeApp.DEFAULT.isBlank());
    }
}
