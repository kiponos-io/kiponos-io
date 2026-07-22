package io.kiponos.examples.retail;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RetailCheckoutTimeoutLogicTest {

    @Test
    void clampsTimeoutRange() {
        assertEquals(250, RetailCheckoutTimeoutApp.resolvePolicy(10, false).timeoutMs());
        assertEquals(60_000, RetailCheckoutTimeoutApp.resolvePolicy(999_999, false).timeoutMs());
        assertEquals(5_000, RetailCheckoutTimeoutApp.resolvePolicy(5_000, true).timeoutMs());
    }

    @Test
    void softFailSummary() {
        var p = RetailCheckoutTimeoutApp.resolvePolicy(4000, true);
        assertTrue(p.softFailOnTimeout());
        assertTrue(p.summaryLine().contains("soft-fail ON"));
    }

    @Test
    void hardFailSummary() {
        var p = RetailCheckoutTimeoutApp.resolvePolicy(3000, false);
        assertFalse(p.softFailOnTimeout());
        assertTrue(p.summaryLine().contains("hard fail"));
    }
}
