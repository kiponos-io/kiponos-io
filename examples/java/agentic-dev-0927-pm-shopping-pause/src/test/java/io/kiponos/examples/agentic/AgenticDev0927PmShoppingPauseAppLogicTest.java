package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev0927PmShoppingPauseAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("incident-pause", AgenticDev0927PmShoppingPauseApp.KEY);
        assertEquals("agentic-dev-0927-pm-shopping-pause", AgenticDev0927PmShoppingPauseApp.FOLDER);
        assertEquals("off", AgenticDev0927PmShoppingPauseApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev0927PmShoppingPauseApp.decide(null);
        assertEquals("off", d.value());
        assertEquals("shopping_path_live", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev0927PmShoppingPauseApp.decide("off");
        assertTrue(d.proceed());
        assertEquals("shopping_path_live", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev0927PmShoppingPauseApp.decide("on");
        assertFalse(blocked.proceed());
        assertEquals("freeze_shopping_writes", blocked.action());
    }
}
