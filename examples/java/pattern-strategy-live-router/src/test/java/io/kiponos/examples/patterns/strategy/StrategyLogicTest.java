package io.kiponos.examples.patterns.strategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StrategyLogicTest {

    @Test
    void flatLeavesCartUnchanged() {
        var ctx = new StrategyLiveRouterApp.StrategyContext(12_500L, false, 10_000, 150);
        var flat = new StrategyLiveRouterApp.FlatPricingStrategy();
        assertEquals(12_500L, flat.priceCents(ctx));
    }

    @Test
    void volumeDiscountsOnlyAboveThreshold() {
        var volume = new StrategyLiveRouterApp.VolumePricingStrategy();
        var under = new StrategyLiveRouterApp.StrategyContext(9_000L, false, 10_000, 150);
        var over = new StrategyLiveRouterApp.StrategyContext(12_500L, false, 10_000, 150);
        assertEquals(9_000L, volume.priceCents(under));
        assertEquals(11_875L, volume.priceCents(over));
    }

    @Test
    void loyaltyAppliesBpsOnlyForMembers() {
        var loyalty = new StrategyLiveRouterApp.LoyaltyPricingStrategy();
        var guest = new StrategyLiveRouterApp.StrategyContext(10_000L, false, 10_000, 150);
        var member = new StrategyLiveRouterApp.StrategyContext(10_000L, true, 10_000, 150);
        assertEquals(10_000L, loyalty.priceCents(guest));
        assertEquals(9_850L, loyalty.priceCents(member));
    }

    @Test
    void truthinessMatchesOpsConventions() {
        assertTrue(StrategyLiveRouterApp.isTruthy("yes"));
        assertTrue(StrategyLiveRouterApp.isTruthy("TRUE"));
        assertFalse(StrategyLiveRouterApp.isTruthy("no"));
        assertFalse(StrategyLiveRouterApp.isTruthy(null));
    }
}
