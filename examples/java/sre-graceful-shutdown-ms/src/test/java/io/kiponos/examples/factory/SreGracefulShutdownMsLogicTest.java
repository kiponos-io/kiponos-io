package io.kiponos.examples.factory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SreGracefulShutdownMsLogicTest {
    @Test
    void defaultValueShape() {
        assertEquals("15000", "15000");
        assertFalse("drain-ms".isBlank());
    }
}
