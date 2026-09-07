package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0913PmLlmSkillBudgetAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("skill-budget", AgenticMcp0913PmLlmSkillBudgetApp.KEY);
        assertEquals("agentic-mcp-0913-pm-llm-skill-budget", AgenticMcp0913PmLlmSkillBudgetApp.FOLDER);
        assertEquals("on", AgenticMcp0913PmLlmSkillBudgetApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0913PmLlmSkillBudgetApp.decide(null);
        assertEquals("on", d.value());
        assertEquals("generated_skills_ok", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0913PmLlmSkillBudgetApp.decide("off");
        assertTrue(d.proceed());
        assertEquals("generated_skills_ok", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMcp0913PmLlmSkillBudgetApp.decide("on");
        assertFalse(blocked.proceed());
        assertEquals("generated_skills_blocked", blocked.action());
    }
}
