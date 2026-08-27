package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev0919PmAdminWallAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("wall-focus", AgenticDev0919PmAdminWallApp.KEY);
        assertEquals("agentic-dev-0919-pm-admin-wall", AgenticDev0919PmAdminWallApp.FOLDER);
        assertEquals("checkout", AgenticDev0919PmAdminWallApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev0919PmAdminWallApp.decide(null);
        assertEquals("checkout", d.value());
        assertEquals("admin_wall_focus", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev0919PmAdminWallApp.decide("checkout");
        assertTrue(d.proceed());
        assertEquals("admin_wall_focus", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev0919PmAdminWallApp.decide("idle");
        assertFalse(blocked.proceed());
        assertEquals("admin_wall_idle", blocked.action());
    }
}
