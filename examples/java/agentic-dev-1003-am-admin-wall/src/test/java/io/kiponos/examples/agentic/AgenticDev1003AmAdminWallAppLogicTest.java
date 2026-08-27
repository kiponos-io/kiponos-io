package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev1003AmAdminWallAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("wall-focus", AgenticDev1003AmAdminWallApp.KEY);
        assertEquals("agentic-dev-1003-am-admin-wall", AgenticDev1003AmAdminWallApp.FOLDER);
        assertEquals("checkout", AgenticDev1003AmAdminWallApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev1003AmAdminWallApp.decide(null);
        assertEquals("checkout", d.value());
        assertEquals("admin_wall_focus", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev1003AmAdminWallApp.decide("checkout");
        assertTrue(d.proceed());
        assertEquals("admin_wall_focus", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev1003AmAdminWallApp.decide("idle");
        assertFalse(blocked.proceed());
        assertEquals("admin_wall_idle", blocked.action());
    }
}
