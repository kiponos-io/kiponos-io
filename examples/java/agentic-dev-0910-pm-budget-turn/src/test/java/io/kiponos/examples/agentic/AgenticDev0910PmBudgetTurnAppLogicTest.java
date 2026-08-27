package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev0910PmBudgetTurnAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("max-tokens", AgenticDev0910PmBudgetTurnApp.KEY);
        assertEquals("agentic-dev-0910-pm-budget-turn", AgenticDev0910PmBudgetTurnApp.FOLDER);
        assertEquals("8000", AgenticDev0910PmBudgetTurnApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev0910PmBudgetTurnApp.decide(null);
        assertEquals("8000", d.value());
        assertEquals("within_token_budget", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev0910PmBudgetTurnApp.decide("8000");
        assertTrue(d.proceed());
        assertEquals("within_token_budget", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev0910PmBudgetTurnApp.decide("0");
        assertFalse(blocked.proceed());
        assertEquals("stop_turn_budget", blocked.action());
    }
}
