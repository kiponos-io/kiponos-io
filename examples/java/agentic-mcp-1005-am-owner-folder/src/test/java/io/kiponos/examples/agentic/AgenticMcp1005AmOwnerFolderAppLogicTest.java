package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp1005AmOwnerFolderAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("owner-agent", AgenticMcp1005AmOwnerFolderApp.KEY);
        assertEquals("agentic-mcp-1005-am-owner-folder", AgenticMcp1005AmOwnerFolderApp.FOLDER);
        assertEquals("travel-coordinator", AgenticMcp1005AmOwnerFolderApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp1005AmOwnerFolderApp.decide(null);
        assertEquals("travel-coordinator", d.value());
        assertEquals("honor_chosen_owner", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp1005AmOwnerFolderApp.decide("travel-coordinator");
        assertTrue(d.proceed());
        assertEquals("honor_chosen_owner", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticMcp1005AmOwnerFolderApp.decide("travel-coordinator");
        assertTrue(dflt.proceed());
    }
}
