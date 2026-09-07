package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0927PmRulesVsSkillsAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("enabled-set", AgenticMcp0927PmRulesVsSkillsApp.KEY);
        assertEquals("agentic-mcp-0927-pm-rules-vs-skills", AgenticMcp0927PmRulesVsSkillsApp.FOLDER);
        assertEquals("research,notify", AgenticMcp0927PmRulesVsSkillsApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0927PmRulesVsSkillsApp.decide(null);
        assertEquals("research,notify", d.value());
        assertEquals("honor_enabled_skills", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0927PmRulesVsSkillsApp.decide("research,notify");
        assertTrue(d.proceed());
        assertEquals("honor_enabled_skills", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticMcp0927PmRulesVsSkillsApp.decide("research,notify");
        assertTrue(dflt.proceed());
    }
}
