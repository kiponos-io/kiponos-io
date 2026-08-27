package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMed1020MiSkillEnableAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("enabled-set", AgenticMed1020MiSkillEnableApp.KEY);
        assertEquals("agentic-med-1020-mi-skill-enable", AgenticMed1020MiSkillEnableApp.FOLDER);
        assertEquals("research,notify", AgenticMed1020MiSkillEnableApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMed1020MiSkillEnableApp.decide(null);
        assertEquals("research,notify", d.value());
        assertEquals("honor_enabled_skills", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMed1020MiSkillEnableApp.decide("research,notify");
        assertTrue(d.proceed());
        assertEquals("honor_enabled_skills", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticMed1020MiSkillEnableApp.decide("research,notify");
        assertTrue(dflt.proceed());
    }
}
