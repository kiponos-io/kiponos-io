package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMed0920AfAgentFolderAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("owner-agent", AgenticMed0920AfAgentFolderApp.KEY);
        assertEquals("agentic-med-0920-af-agent-folder", AgenticMed0920AfAgentFolderApp.FOLDER);
        assertEquals("travel-coordinator", AgenticMed0920AfAgentFolderApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMed0920AfAgentFolderApp.decide(null);
        assertEquals("travel-coordinator", d.value());
        assertEquals("honor_chosen_owner", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMed0920AfAgentFolderApp.decide("travel-coordinator");
        assertTrue(d.proceed());
        assertEquals("honor_chosen_owner", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticMed0920AfAgentFolderApp.decide("travel-coordinator");
        assertTrue(dflt.proceed());
    }
}
