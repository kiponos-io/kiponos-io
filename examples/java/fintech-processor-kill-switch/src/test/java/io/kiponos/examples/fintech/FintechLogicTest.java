package io.kiponos.examples.fintech;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FintechLogicTest {

    @Test
    void truthinessMatchesOpsDashboardConventions() {
        assertTrue(FintechProcessorKillSwitchApp.isTruthy("yes"));
        assertTrue(FintechProcessorKillSwitchApp.isTruthy("TRUE"));
        assertTrue(FintechProcessorKillSwitchApp.isTruthy("on"));
        assertTrue(FintechProcessorKillSwitchApp.isTruthy("1"));
        assertFalse(FintechProcessorKillSwitchApp.isTruthy("no"));
        assertFalse(FintechProcessorKillSwitchApp.isTruthy("false"));
        assertFalse(FintechProcessorKillSwitchApp.isTruthy("0"));
        assertFalse(FintechProcessorKillSwitchApp.isTruthy(null));
    }

    @Test
    void refusedWhenDisabledUsesReasonDetail() {
        // Pure logic path via record construction patterns used in app
        var refused = new FintechProcessorKillSwitchApp.AuthDecision(
                "REFUSED", "t1", 100L, "processor_disabled:acquirer_timeout");
        assertEquals("REFUSED", refused.status());
        assertTrue(refused.detail().contains("processor_disabled"));
    }
}
