package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev0918AmAgentFolderAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("owner-agent", AgenticDev0918AmAgentFolderApp.KEY);
        assertEquals("agentic-dev-0918-am-agent-folder", AgenticDev0918AmAgentFolderApp.FOLDER);
        assertEquals("travel-coordinator", AgenticDev0918AmAgentFolderApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev0918AmAgentFolderApp.decide(null);
        assertEquals("travel-coordinator", d.value());
        assertEquals("honor_chosen_owner", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev0918AmAgentFolderApp.decide("travel-coordinator");
        assertTrue(d.proceed());
        assertEquals("honor_chosen_owner", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticDev0918AmAgentFolderApp.decide("travel-coordinator");
        assertTrue(dflt.proceed());
    }
}
