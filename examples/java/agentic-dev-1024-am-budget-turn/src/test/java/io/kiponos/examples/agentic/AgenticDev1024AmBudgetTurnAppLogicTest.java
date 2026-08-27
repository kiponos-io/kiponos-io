package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev1024AmBudgetTurnAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("max-tokens", AgenticDev1024AmBudgetTurnApp.KEY);
        assertEquals("agentic-dev-1024-am-budget-turn", AgenticDev1024AmBudgetTurnApp.FOLDER);
        assertEquals("8000", AgenticDev1024AmBudgetTurnApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev1024AmBudgetTurnApp.decide(null);
        assertEquals("8000", d.value());
        assertEquals("within_token_budget", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev1024AmBudgetTurnApp.decide("8000");
        assertTrue(d.proceed());
        assertEquals("within_token_budget", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev1024AmBudgetTurnApp.decide("0");
        assertFalse(blocked.proceed());
        assertEquals("stop_turn_budget", blocked.action());
    }
}
