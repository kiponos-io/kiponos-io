package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMed0920MiSkillEnableAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("enabled-set", AgenticMed0920MiSkillEnableApp.KEY);
        assertEquals("agentic-med-0920-mi-skill-enable", AgenticMed0920MiSkillEnableApp.FOLDER);
        assertEquals("research,notify", AgenticMed0920MiSkillEnableApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMed0920MiSkillEnableApp.decide(null);
        assertEquals("research,notify", d.value());
        assertEquals("honor_enabled_skills", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMed0920MiSkillEnableApp.decide("research,notify");
        assertTrue(d.proceed());
        assertEquals("honor_enabled_skills", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticMed0920MiSkillEnableApp.decide("research,notify");
        assertTrue(dflt.proceed());
    }
}
