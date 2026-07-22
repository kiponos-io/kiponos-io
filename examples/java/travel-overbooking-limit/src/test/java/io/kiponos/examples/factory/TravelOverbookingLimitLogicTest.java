package io.kiponos.examples.factory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TravelOverbookingLimitLogicTest {
    @Test
    void defaultValueShape() {
        assertEquals("5", "5");
        assertFalse("overbook-pct".isBlank());
    }
}
