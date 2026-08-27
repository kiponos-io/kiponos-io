package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev0904AmBudgetTurnAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("max-tokens", AgenticDev0904AmBudgetTurnApp.KEY);
        assertEquals("agentic-dev-0904-am-budget-turn", AgenticDev0904AmBudgetTurnApp.FOLDER);
        assertEquals("8000", AgenticDev0904AmBudgetTurnApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev0904AmBudgetTurnApp.decide(null);
        assertEquals("8000", d.value());
        assertEquals("within_token_budget", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev0904AmBudgetTurnApp.decide("8000");
        assertTrue(d.proceed());
        assertEquals("within_token_budget", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev0904AmBudgetTurnApp.decide("0");
        assertFalse(blocked.proceed());
        assertEquals("stop_turn_budget", blocked.action());
    }
}
