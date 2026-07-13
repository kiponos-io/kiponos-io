package io.kiponos.examples.killswitch;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KillSwitchLogicTest {
    @Test
    void parsesYesNoTruthiness() {
        assertTrue(isOn("yes"));
        assertTrue(isOn("TRUE"));
        assertTrue(isOn("on"));
        assertTrue(isOn("1"));
        assertFalse(isOn("no"));
        assertFalse(isOn("false"));
        assertFalse(isOn("0"));
    }

    private static boolean isOn(String raw) {
        return "yes".equalsIgnoreCase(raw)
                || "true".equalsIgnoreCase(raw)
                || "on".equalsIgnoreCase(raw)
                || "1".equals(raw);
    }
}
