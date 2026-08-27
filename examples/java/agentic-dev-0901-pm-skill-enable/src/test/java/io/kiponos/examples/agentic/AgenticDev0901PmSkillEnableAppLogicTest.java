package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev0901PmSkillEnableAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("enabled-set", AgenticDev0901PmSkillEnableApp.KEY);
        assertEquals("agentic-dev-0901-pm-skill-enable", AgenticDev0901PmSkillEnableApp.FOLDER);
        assertEquals("research,notify", AgenticDev0901PmSkillEnableApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev0901PmSkillEnableApp.decide(null);
        assertEquals("research,notify", d.value());
        assertEquals("honor_enabled_skills", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev0901PmSkillEnableApp.decide("research,notify");
        assertTrue(d.proceed());
        assertEquals("honor_enabled_skills", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticDev0901PmSkillEnableApp.decide("research,notify");
        assertTrue(dflt.proceed());
    }
}
