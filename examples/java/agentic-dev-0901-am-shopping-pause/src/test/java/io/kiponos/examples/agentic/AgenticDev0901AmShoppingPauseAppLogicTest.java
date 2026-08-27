package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev0901AmShoppingPauseAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("incident-pause", AgenticDev0901AmShoppingPauseApp.KEY);
        assertEquals("agentic-dev-0901-am-shopping-pause", AgenticDev0901AmShoppingPauseApp.FOLDER);
        assertEquals("off", AgenticDev0901AmShoppingPauseApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev0901AmShoppingPauseApp.decide(null);
        assertEquals("off", d.value());
        assertEquals("shopping_path_live", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev0901AmShoppingPauseApp.decide("off");
        assertTrue(d.proceed());
        assertEquals("shopping_path_live", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev0901AmShoppingPauseApp.decide("on");
        assertFalse(blocked.proceed());
        assertEquals("freeze_shopping_writes", blocked.action());
    }
}
