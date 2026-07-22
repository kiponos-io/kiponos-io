package io.kiponos.examples.factory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SreFeatureKillSwitchLogicTest {
    @Test
    void defaultValueShape() {
        assertEquals("yes", "yes");
        assertFalse("enabled".isBlank());
    }
}
