package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev1027PmShoppingPauseAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("incident-pause", AgenticDev1027PmShoppingPauseApp.KEY);
        assertEquals("agentic-dev-1027-pm-shopping-pause", AgenticDev1027PmShoppingPauseApp.FOLDER);
        assertEquals("off", AgenticDev1027PmShoppingPauseApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev1027PmShoppingPauseApp.decide(null);
        assertEquals("off", d.value());
        assertEquals("shopping_path_live", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev1027PmShoppingPauseApp.decide("off");
        assertTrue(d.proceed());
        assertEquals("shopping_path_live", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev1027PmShoppingPauseApp.decide("on");
        assertFalse(blocked.proceed());
        assertEquals("freeze_shopping_writes", blocked.action());
    }
}
