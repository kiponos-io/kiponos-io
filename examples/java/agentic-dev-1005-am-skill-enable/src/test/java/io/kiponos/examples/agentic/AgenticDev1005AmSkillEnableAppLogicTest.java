package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev1005AmSkillEnableAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("enabled-set", AgenticDev1005AmSkillEnableApp.KEY);
        assertEquals("agentic-dev-1005-am-skill-enable", AgenticDev1005AmSkillEnableApp.FOLDER);
        assertEquals("research,notify", AgenticDev1005AmSkillEnableApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev1005AmSkillEnableApp.decide(null);
        assertEquals("research,notify", d.value());
        assertEquals("honor_enabled_skills", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev1005AmSkillEnableApp.decide("research,notify");
        assertTrue(d.proceed());
        assertEquals("honor_enabled_skills", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticDev1005AmSkillEnableApp.decide("research,notify");
        assertTrue(dflt.proceed());
    }
}
