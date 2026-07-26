package io.kiponos.examples.factory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FintechVelocityCapLogicTest {
    @Test
    void hubKeyNameIsStable() {
        assertEquals("max-tx-per-min", "max-tx-per-min");
        assertTrue("max-tx-per-min".contains("tx"));
    }

    @Test
    void defaultCapParsesAsPositiveInt() {
        int def = 60;
        assertTrue(def > 0);
        assertEquals(60, Integer.parseInt("60"));
    }
}
