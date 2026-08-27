package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMed1026AfBudgetTurnAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("max-tokens", AgenticMed1026AfBudgetTurnApp.KEY);
        assertEquals("agentic-med-1026-af-budget-turn", AgenticMed1026AfBudgetTurnApp.FOLDER);
        assertEquals("8000", AgenticMed1026AfBudgetTurnApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMed1026AfBudgetTurnApp.decide(null);
        assertEquals("8000", d.value());
        assertEquals("within_token_budget", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMed1026AfBudgetTurnApp.decide("8000");
        assertTrue(d.proceed());
        assertEquals("within_token_budget", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMed1026AfBudgetTurnApp.decide("0");
        assertFalse(blocked.proceed());
        assertEquals("stop_turn_budget", blocked.action());
    }
}
