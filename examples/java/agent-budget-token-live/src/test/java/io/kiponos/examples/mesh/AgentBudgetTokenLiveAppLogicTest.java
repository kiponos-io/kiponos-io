package io.kiponos.examples.mesh;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgentBudgetTokenLiveAppLogicTest {
    @Test
    void hubKeyIsStable() {
        assertEquals("max-tokens-per-turn", AgentBudgetTokenLiveApp.KEY);
        assertFalse(AgentBudgetTokenLiveApp.KEY.isBlank());
    }

    @Test
    void defaultIsNonEmpty() {
        assertFalse(AgentBudgetTokenLiveApp.DEFAULT.isBlank());
    }
}
