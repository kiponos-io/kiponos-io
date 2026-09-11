package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev1031PmSkillEnableAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("enabled-set", AgenticDev1031PmSkillEnableApp.KEY);
        assertEquals("agentic-dev-1031-pm-skill-enable", AgenticDev1031PmSkillEnableApp.FOLDER);
        assertEquals("research,notify", AgenticDev1031PmSkillEnableApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev1031PmSkillEnableApp.decide(null);
        assertEquals("research,notify", d.value());
        assertEquals("honor_enabled_skills", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev1031PmSkillEnableApp.decide("research,notify");
        assertTrue(d.proceed());
        assertEquals("honor_enabled_skills", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticDev1031PmSkillEnableApp.decide("research,notify");
        assertTrue(dflt.proceed());
    }
}
