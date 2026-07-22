package io.kiponos.examples.offline;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OfflineLkgLogicTest {

    @Test
    void liveUsesHubValue() {
        var p = OfflineLkgApp.resolvePosture(OfflineLkgApp.SdkMode.LIVE, 3000, 2500);
        assertEquals(3000, p.effectiveTimeoutMs());
        assertEquals("live-hub", p.source());
        assertTrue(p.summaryLine().contains("LIVE"));
    }

    @Test
    void offlineUsesLastKnownGood() {
        var p = OfflineLkgApp.resolvePosture(OfflineLkgApp.SdkMode.OFFLINE, 4000, 2500);
        assertEquals(4000, p.effectiveTimeoutMs());
        assertEquals("last-known-good", p.source());
        assertTrue(p.summaryLine().contains("LKG"));
    }

    @Test
    void safeUsesConservativeDefault() {
        var p = OfflineLkgApp.resolvePosture(OfflineLkgApp.SdkMode.SAFE, 99999, 2500);
        assertEquals(2500, p.effectiveTimeoutMs());
        assertEquals("safe-default", p.source());
    }

    @Test
    void parsePositiveInt() {
        assertEquals(10, OfflineLkgApp.parsePositiveInt("10", 1));
        assertEquals(1, OfflineLkgApp.parsePositiveInt("0", 1));
        assertEquals(1, OfflineLkgApp.parsePositiveInt("x", 1));
    }
}
