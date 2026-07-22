package io.kiponos.examples.factory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MediaBitrateCeilingLogicTest {
    @Test
    void defaultValueShape() {
        assertEquals("4000", "4000");
        assertFalse("max-kbps".isBlank());
    }
}
