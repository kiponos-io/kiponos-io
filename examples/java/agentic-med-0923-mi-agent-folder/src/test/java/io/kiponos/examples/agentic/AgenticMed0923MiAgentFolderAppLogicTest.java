package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMed0923MiAgentFolderAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("owner-agent", AgenticMed0923MiAgentFolderApp.KEY);
        assertEquals("agentic-med-0923-mi-agent-folder", AgenticMed0923MiAgentFolderApp.FOLDER);
        assertEquals("travel-coordinator", AgenticMed0923MiAgentFolderApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMed0923MiAgentFolderApp.decide(null);
        assertEquals("travel-coordinator", d.value());
        assertEquals("honor_chosen_owner", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMed0923MiAgentFolderApp.decide("travel-coordinator");
        assertTrue(d.proceed());
        assertEquals("honor_chosen_owner", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticMed0923MiAgentFolderApp.decide("travel-coordinator");
        assertTrue(dflt.proceed());
    }
}
