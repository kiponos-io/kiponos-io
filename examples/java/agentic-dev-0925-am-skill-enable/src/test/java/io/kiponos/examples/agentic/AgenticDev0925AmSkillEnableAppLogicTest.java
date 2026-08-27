package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev0925AmSkillEnableAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("enabled-set", AgenticDev0925AmSkillEnableApp.KEY);
        assertEquals("agentic-dev-0925-am-skill-enable", AgenticDev0925AmSkillEnableApp.FOLDER);
        assertEquals("research,notify", AgenticDev0925AmSkillEnableApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev0925AmSkillEnableApp.decide(null);
        assertEquals("research,notify", d.value());
        assertEquals("honor_enabled_skills", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev0925AmSkillEnableApp.decide("research,notify");
        assertTrue(d.proceed());
        assertEquals("honor_enabled_skills", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticDev0925AmSkillEnableApp.decide("research,notify");
        assertTrue(dflt.proceed());
    }
}
