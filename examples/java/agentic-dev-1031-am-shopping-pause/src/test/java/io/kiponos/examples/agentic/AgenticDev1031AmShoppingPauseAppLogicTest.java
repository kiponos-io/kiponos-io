package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev1031AmShoppingPauseAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("incident-pause", AgenticDev1031AmShoppingPauseApp.KEY);
        assertEquals("agentic-dev-1031-am-shopping-pause", AgenticDev1031AmShoppingPauseApp.FOLDER);
        assertEquals("off", AgenticDev1031AmShoppingPauseApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev1031AmShoppingPauseApp.decide(null);
        assertEquals("off", d.value());
        assertEquals("shopping_path_live", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev1031AmShoppingPauseApp.decide("off");
        assertTrue(d.proceed());
        assertEquals("shopping_path_live", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev1031AmShoppingPauseApp.decide("on");
        assertFalse(blocked.proceed());
        assertEquals("freeze_shopping_writes", blocked.action());
    }
}
