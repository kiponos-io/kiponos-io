package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev1029PmAdminWallAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("wall-focus", AgenticDev1029PmAdminWallApp.KEY);
        assertEquals("agentic-dev-1029-pm-admin-wall", AgenticDev1029PmAdminWallApp.FOLDER);
        assertEquals("checkout", AgenticDev1029PmAdminWallApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev1029PmAdminWallApp.decide(null);
        assertEquals("checkout", d.value());
        assertEquals("admin_wall_focus", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev1029PmAdminWallApp.decide("checkout");
        assertTrue(d.proceed());
        assertEquals("admin_wall_focus", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev1029PmAdminWallApp.decide("idle");
        assertFalse(blocked.proceed());
        assertEquals("admin_wall_idle", blocked.action());
    }
}
