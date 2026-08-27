package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev1015AmSkillEnableAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("enabled-set", AgenticDev1015AmSkillEnableApp.KEY);
        assertEquals("agentic-dev-1015-am-skill-enable", AgenticDev1015AmSkillEnableApp.FOLDER);
        assertEquals("research,notify", AgenticDev1015AmSkillEnableApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev1015AmSkillEnableApp.decide(null);
        assertEquals("research,notify", d.value());
        assertEquals("honor_enabled_skills", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev1015AmSkillEnableApp.decide("research,notify");
        assertTrue(d.proceed());
        assertEquals("honor_enabled_skills", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticDev1015AmSkillEnableApp.decide("research,notify");
        assertTrue(dflt.proceed());
    }
}
