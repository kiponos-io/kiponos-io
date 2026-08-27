package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev1014PmAgentFolderAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("owner-agent", AgenticDev1014PmAgentFolderApp.KEY);
        assertEquals("agentic-dev-1014-pm-agent-folder", AgenticDev1014PmAgentFolderApp.FOLDER);
        assertEquals("travel-coordinator", AgenticDev1014PmAgentFolderApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev1014PmAgentFolderApp.decide(null);
        assertEquals("travel-coordinator", d.value());
        assertEquals("honor_chosen_owner", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev1014PmAgentFolderApp.decide("travel-coordinator");
        assertTrue(d.proceed());
        assertEquals("honor_chosen_owner", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticDev1014PmAgentFolderApp.decide("travel-coordinator");
        assertTrue(dflt.proceed());
    }
}
