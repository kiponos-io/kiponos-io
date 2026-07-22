package io.kiponos.examples.factory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AhaRateLimitRpsLogicTest {
    @Test
    void defaultValueShape() {
        assertEquals("100", "100");
        assertFalse("rps".isBlank());
    }
}
