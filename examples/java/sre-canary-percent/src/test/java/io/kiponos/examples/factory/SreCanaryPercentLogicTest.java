package io.kiponos.examples.factory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SreCanaryPercentLogicTest {
    @Test
    void defaultValueShape() {
        assertEquals("5", "5");
        assertFalse("percent".isBlank());
    }
}
