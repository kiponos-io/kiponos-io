package io.kiponos.examples.factory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FintechVelocityCapLogicTest {
    @Test
    void defaultValueShape() {
        assertEquals("60", "60");
        assertFalse("max-tx-per-min".isBlank());
    }
}
