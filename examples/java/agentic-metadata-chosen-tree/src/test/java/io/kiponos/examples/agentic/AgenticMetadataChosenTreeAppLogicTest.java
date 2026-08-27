package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMetadataChosenTreeAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("owner-agent", AgenticMetadataChosenTreeApp.KEY);
        assertEquals("agentic-metadata-chosen-tree", AgenticMetadataChosenTreeApp.FOLDER);
        assertEquals("travel-coordinator", AgenticMetadataChosenTreeApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMetadataChosenTreeApp.decide(null);
        assertEquals("travel-coordinator", d.value());
        assertEquals("honor_chosen_owner", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMetadataChosenTreeApp.decide("travel-coordinator");
        assertTrue(d.proceed());
        assertEquals("honor_chosen_owner", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticMetadataChosenTreeApp.decide("travel-coordinator");
        assertTrue(dflt.proceed());
    }
}
