package io.kiponos.examples.patterns.chain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChainLogicTest {

    @Test
    void csvSplitsAndNormalizes() {
        assertEquals(List.of("amount-cap", "geo"), ChainLiveFraudApp.csv(" amount-cap, GEO "));
    }

    @Test
    void passDecisionFactory() {
        var d = ChainLiveFraudApp.Decision.pass("geo");
        assertTrue(d.allowed());
    }

    @Test
    void rejectDecisionFactory() {
        var d = ChainLiveFraudApp.Decision.reject("velocity", "too fast");
        assertFalse(d.allowed());
        assertEquals("too fast", d.reason());
    }
}
