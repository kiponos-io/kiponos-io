package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev1023AmAdminWallAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("wall-focus", AgenticDev1023AmAdminWallApp.KEY);
        assertEquals("agentic-dev-1023-am-admin-wall", AgenticDev1023AmAdminWallApp.FOLDER);
        assertEquals("checkout", AgenticDev1023AmAdminWallApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev1023AmAdminWallApp.decide(null);
        assertEquals("checkout", d.value());
        assertEquals("admin_wall_focus", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev1023AmAdminWallApp.decide("checkout");
        assertTrue(d.proceed());
        assertEquals("admin_wall_focus", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev1023AmAdminWallApp.decide("idle");
        assertFalse(blocked.proceed());
        assertEquals("admin_wall_idle", blocked.action());
    }
}
