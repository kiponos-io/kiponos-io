package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMed1017AfSkillEnableAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("enabled-set", AgenticMed1017AfSkillEnableApp.KEY);
        assertEquals("agentic-med-1017-af-skill-enable", AgenticMed1017AfSkillEnableApp.FOLDER);
        assertEquals("research,notify", AgenticMed1017AfSkillEnableApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMed1017AfSkillEnableApp.decide(null);
        assertEquals("research,notify", d.value());
        assertEquals("honor_enabled_skills", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMed1017AfSkillEnableApp.decide("research,notify");
        assertTrue(d.proceed());
        assertEquals("honor_enabled_skills", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticMed1017AfSkillEnableApp.decide("research,notify");
        assertTrue(dflt.proceed());
    }
}
