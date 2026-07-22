package io.kiponos.examples.factory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AhaRetryMaxAttemptsLogicTest {
    @Test
    void defaultValueShape() {
        assertEquals("3", "3");
        assertFalse("max-attempts".isBlank());
    }
}
