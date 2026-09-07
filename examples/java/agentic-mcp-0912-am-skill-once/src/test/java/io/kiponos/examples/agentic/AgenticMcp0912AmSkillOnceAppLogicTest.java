package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0912AmSkillOnceAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("enabled-set", AgenticMcp0912AmSkillOnceApp.KEY);
        assertEquals("agentic-mcp-0912-am-skill-once", AgenticMcp0912AmSkillOnceApp.FOLDER);
        assertEquals("research,notify", AgenticMcp0912AmSkillOnceApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0912AmSkillOnceApp.decide(null);
        assertEquals("research,notify", d.value());
        assertEquals("honor_enabled_skills", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0912AmSkillOnceApp.decide("research,notify");
        assertTrue(d.proceed());
        assertEquals("honor_enabled_skills", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticMcp0912AmSkillOnceApp.decide("research,notify");
        assertTrue(dflt.proceed());
    }
}
