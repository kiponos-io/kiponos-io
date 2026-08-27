package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMed1010AfAgentFolderAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("owner-agent", AgenticMed1010AfAgentFolderApp.KEY);
        assertEquals("agentic-med-1010-af-agent-folder", AgenticMed1010AfAgentFolderApp.FOLDER);
        assertEquals("travel-coordinator", AgenticMed1010AfAgentFolderApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMed1010AfAgentFolderApp.decide(null);
        assertEquals("travel-coordinator", d.value());
        assertEquals("honor_chosen_owner", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMed1010AfAgentFolderApp.decide("travel-coordinator");
        assertTrue(d.proceed());
        assertEquals("honor_chosen_owner", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticMed1010AfAgentFolderApp.decide("travel-coordinator");
        assertTrue(dflt.proceed());
    }
}
