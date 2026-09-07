package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0913AmSkillTrustAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("skill-trust", AgenticMcp0913AmSkillTrustApp.KEY);
        assertEquals("agentic-mcp-0913-am-skill-trust", AgenticMcp0913AmSkillTrustApp.FOLDER);
        assertEquals("core,reviewed", AgenticMcp0913AmSkillTrustApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0913AmSkillTrustApp.decide(null);
        assertEquals("core,reviewed", d.value());
        assertEquals("honor_trusted_skills", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0913AmSkillTrustApp.decide("core,reviewed");
        assertTrue(d.proceed());
        assertEquals("honor_trusted_skills", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticMcp0913AmSkillTrustApp.decide("core,reviewed");
        assertTrue(dflt.proceed());
    }
}
