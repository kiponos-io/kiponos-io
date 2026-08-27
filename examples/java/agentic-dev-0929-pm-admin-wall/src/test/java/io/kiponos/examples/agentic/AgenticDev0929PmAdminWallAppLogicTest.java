package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev0929PmAdminWallAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("wall-focus", AgenticDev0929PmAdminWallApp.KEY);
        assertEquals("agentic-dev-0929-pm-admin-wall", AgenticDev0929PmAdminWallApp.FOLDER);
        assertEquals("checkout", AgenticDev0929PmAdminWallApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev0929PmAdminWallApp.decide(null);
        assertEquals("checkout", d.value());
        assertEquals("admin_wall_focus", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev0929PmAdminWallApp.decide("checkout");
        assertTrue(d.proceed());
        assertEquals("admin_wall_focus", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev0929PmAdminWallApp.decide("idle");
        assertFalse(blocked.proceed());
        assertEquals("admin_wall_idle", blocked.action());
    }
}
