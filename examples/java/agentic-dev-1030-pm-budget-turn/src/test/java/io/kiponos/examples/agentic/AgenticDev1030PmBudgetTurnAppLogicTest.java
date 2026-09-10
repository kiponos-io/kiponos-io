package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev1030PmBudgetTurnAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("max-tokens", AgenticDev1030PmBudgetTurnApp.KEY);
        assertEquals("agentic-dev-1030-pm-budget-turn", AgenticDev1030PmBudgetTurnApp.FOLDER);
        assertEquals("8000", AgenticDev1030PmBudgetTurnApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev1030PmBudgetTurnApp.decide(null);
        assertEquals("8000", d.value());
        assertEquals("within_token_budget", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev1030PmBudgetTurnApp.decide("8000");
        assertTrue(d.proceed());
        assertEquals("within_token_budget", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev1030PmBudgetTurnApp.decide("0");
        assertFalse(blocked.proceed());
        assertEquals("stop_turn_budget", blocked.action());
    }
}
