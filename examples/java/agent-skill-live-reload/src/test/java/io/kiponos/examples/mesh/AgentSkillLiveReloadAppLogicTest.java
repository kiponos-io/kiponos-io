package io.kiponos.examples.mesh;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgentSkillLiveReloadAppLogicTest {
    @Test
    void hubKeyIsStable() {
        assertEquals("enabled-set", AgentSkillLiveReloadApp.KEY);
        assertFalse(AgentSkillLiveReloadApp.KEY.isBlank());
    }

    @Test
    void defaultIsNonEmpty() {
        assertFalse(AgentSkillLiveReloadApp.DEFAULT.isBlank());
    }
}
