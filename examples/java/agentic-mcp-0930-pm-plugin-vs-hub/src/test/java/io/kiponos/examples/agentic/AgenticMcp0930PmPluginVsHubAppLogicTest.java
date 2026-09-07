package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp0930PmPluginVsHubAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("enabled-set", AgenticMcp0930PmPluginVsHubApp.KEY);
        assertEquals("agentic-mcp-0930-pm-plugin-vs-hub", AgenticMcp0930PmPluginVsHubApp.FOLDER);
        assertEquals("research,notify", AgenticMcp0930PmPluginVsHubApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp0930PmPluginVsHubApp.decide(null);
        assertEquals("research,notify", d.value());
        assertEquals("honor_enabled_skills", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp0930PmPluginVsHubApp.decide("research,notify");
        assertTrue(d.proceed());
        assertEquals("honor_enabled_skills", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticMcp0930PmPluginVsHubApp.decide("research,notify");
        assertTrue(dflt.proceed());
    }
}
