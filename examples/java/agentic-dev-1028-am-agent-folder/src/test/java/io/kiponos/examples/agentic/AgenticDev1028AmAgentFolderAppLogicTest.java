package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev1028AmAgentFolderAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("owner-agent", AgenticDev1028AmAgentFolderApp.KEY);
        assertEquals("agentic-dev-1028-am-agent-folder", AgenticDev1028AmAgentFolderApp.FOLDER);
        assertEquals("travel-coordinator", AgenticDev1028AmAgentFolderApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev1028AmAgentFolderApp.decide(null);
        assertEquals("travel-coordinator", d.value());
        assertEquals("honor_chosen_owner", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev1028AmAgentFolderApp.decide("travel-coordinator");
        assertTrue(d.proceed());
        assertEquals("honor_chosen_owner", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticDev1028AmAgentFolderApp.decide("travel-coordinator");
        assertTrue(dflt.proceed());
    }
}
