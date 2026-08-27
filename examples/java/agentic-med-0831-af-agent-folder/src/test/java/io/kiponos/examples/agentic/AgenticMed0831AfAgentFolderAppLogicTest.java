package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMed0831AfAgentFolderAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("owner-agent", AgenticMed0831AfAgentFolderApp.KEY);
        assertEquals("agentic-med-0831-af-agent-folder", AgenticMed0831AfAgentFolderApp.FOLDER);
        assertEquals("travel-coordinator", AgenticMed0831AfAgentFolderApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMed0831AfAgentFolderApp.decide(null);
        assertEquals("travel-coordinator", d.value());
        assertEquals("honor_chosen_owner", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMed0831AfAgentFolderApp.decide("travel-coordinator");
        assertTrue(d.proceed());
        assertEquals("honor_chosen_owner", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticMed0831AfAgentFolderApp.decide("travel-coordinator");
        assertTrue(dflt.proceed());
    }
}
