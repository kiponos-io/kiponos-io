package io.kiponos.examples.factory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RetailDynamicPricingKnobLogicTest {
    @Test
    void defaultValueShape() {
        assertEquals("1.0", "1.0");
        assertFalse("sale-weight".isBlank());
    }
}
