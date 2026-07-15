package io.kiponos.examples.sre;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DegradationModeLogicTest {

    @Test
    void normalizeModeAliases() {
        assertEquals("full", DegradationModeApp.normalizeMode("FULL"));
        assertEquals("read-only", DegradationModeApp.normalizeMode("read-only"));
        assertEquals("read-only", DegradationModeApp.normalizeMode("readonly"));
        assertEquals("read-only", DegradationModeApp.normalizeMode("RO"));
        assertEquals("maintenance", DegradationModeApp.normalizeMode("maintenance"));
        assertEquals("maintenance", DegradationModeApp.normalizeMode("maint"));
        assertEquals("full", DegradationModeApp.normalizeMode("garbage"));
        assertEquals("full", DegradationModeApp.normalizeMode(null));
        assertEquals("full", DegradationModeApp.normalizeMode("  "));
    }

    @Test
    void parseTruthiness() {
        assertTrue(DegradationModeApp.parseTruth("yes"));
        assertTrue(DegradationModeApp.parseTruth("TRUE"));
        assertTrue(DegradationModeApp.parseTruth("on"));
        assertTrue(DegradationModeApp.parseTruth("1"));
        assertFalse(DegradationModeApp.parseTruth("no"));
        assertFalse(DegradationModeApp.parseTruth("false"));
        assertFalse(DegradationModeApp.parseTruth("0"));
        assertFalse(DegradationModeApp.parseTruth(null));
    }

    @Test
    void fullModeHonorsKnobs() {
        DegradationModeApp.Posture open = DegradationModeApp.resolvePosture("full", true, true);
        assertEquals("full", open.mode());
        assertTrue(open.acceptWrites());
        assertTrue(open.backgroundJobs());

        DegradationModeApp.Posture closed = DegradationModeApp.resolvePosture("full", false, false);
        assertFalse(closed.acceptWrites());
        assertFalse(closed.backgroundJobs());
    }

    @Test
    void readOnlyForcesWritesOffKeepsJobsKnob() {
        DegradationModeApp.Posture withJobs = DegradationModeApp.resolvePosture("read-only", true, true);
        assertEquals("read-only", withJobs.mode());
        assertFalse(withJobs.acceptWrites());
        assertTrue(withJobs.backgroundJobs());

        DegradationModeApp.Posture noJobs = DegradationModeApp.resolvePosture("readonly", true, false);
        assertEquals("read-only", noJobs.mode());
        assertFalse(noJobs.acceptWrites());
        assertFalse(noJobs.backgroundJobs());
    }

    @Test
    void maintenanceForcesEverythingOff() {
        DegradationModeApp.Posture p = DegradationModeApp.resolvePosture("maintenance", true, true);
        assertEquals("maintenance", p.mode());
        assertFalse(p.acceptWrites());
        assertFalse(p.backgroundJobs());
        assertTrue(p.summaryLine().contains("MAINTENANCE"));
    }

    @Test
    void summaryMentionsReadOnly() {
        DegradationModeApp.Posture p = DegradationModeApp.resolvePosture("read-only", true, true);
        assertTrue(p.summaryLine().contains("READ-ONLY"));
    }
}
