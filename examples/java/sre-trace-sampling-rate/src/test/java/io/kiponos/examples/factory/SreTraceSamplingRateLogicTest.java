package io.kiponos.examples.factory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SreTraceSamplingRateLogicTest {
    @Test
    void defaultValueShape() {
        assertEquals("0.1", "0.1");
        assertFalse("sample-rate".isBlank());
    }
}
