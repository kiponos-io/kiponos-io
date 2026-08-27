package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev0924PmAgentFolderAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("owner-agent", AgenticDev0924PmAgentFolderApp.KEY);
        assertEquals("agentic-dev-0924-pm-agent-folder", AgenticDev0924PmAgentFolderApp.FOLDER);
        assertEquals("travel-coordinator", AgenticDev0924PmAgentFolderApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev0924PmAgentFolderApp.decide(null);
        assertEquals("travel-coordinator", d.value());
        assertEquals("honor_chosen_owner", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev0924PmAgentFolderApp.decide("travel-coordinator");
        assertTrue(d.proceed());
        assertEquals("honor_chosen_owner", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticDev0924PmAgentFolderApp.decide("travel-coordinator");
        assertTrue(dflt.proceed());
    }
}
