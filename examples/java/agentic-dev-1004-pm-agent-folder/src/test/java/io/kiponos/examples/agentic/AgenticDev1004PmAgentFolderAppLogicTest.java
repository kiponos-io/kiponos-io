package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev1004PmAgentFolderAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("owner-agent", AgenticDev1004PmAgentFolderApp.KEY);
        assertEquals("agentic-dev-1004-pm-agent-folder", AgenticDev1004PmAgentFolderApp.FOLDER);
        assertEquals("travel-coordinator", AgenticDev1004PmAgentFolderApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev1004PmAgentFolderApp.decide(null);
        assertEquals("travel-coordinator", d.value());
        assertEquals("honor_chosen_owner", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev1004PmAgentFolderApp.decide("travel-coordinator");
        assertTrue(d.proceed());
        assertEquals("honor_chosen_owner", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticDev1004PmAgentFolderApp.decide("travel-coordinator");
        assertTrue(dflt.proceed());
    }
}
