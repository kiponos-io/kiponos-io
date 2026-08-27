package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMed1007AfSkillEnableAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("enabled-set", AgenticMed1007AfSkillEnableApp.KEY);
        assertEquals("agentic-med-1007-af-skill-enable", AgenticMed1007AfSkillEnableApp.FOLDER);
        assertEquals("research,notify", AgenticMed1007AfSkillEnableApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMed1007AfSkillEnableApp.decide(null);
        assertEquals("research,notify", d.value());
        assertEquals("honor_enabled_skills", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMed1007AfSkillEnableApp.decide("research,notify");
        assertTrue(d.proceed());
        assertEquals("honor_enabled_skills", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticMed1007AfSkillEnableApp.decide("research,notify");
        assertTrue(dflt.proceed());
    }
}
