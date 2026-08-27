package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMed0913AfShoppingPauseAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("incident-pause", AgenticMed0913AfShoppingPauseApp.KEY);
        assertEquals("agentic-med-0913-af-shopping-pause", AgenticMed0913AfShoppingPauseApp.FOLDER);
        assertEquals("off", AgenticMed0913AfShoppingPauseApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMed0913AfShoppingPauseApp.decide(null);
        assertEquals("off", d.value());
        assertEquals("shopping_path_live", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMed0913AfShoppingPauseApp.decide("off");
        assertTrue(d.proceed());
        assertEquals("shopping_path_live", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMed0913AfShoppingPauseApp.decide("on");
        assertFalse(blocked.proceed());
        assertEquals("freeze_shopping_writes", blocked.action());
    }
}
