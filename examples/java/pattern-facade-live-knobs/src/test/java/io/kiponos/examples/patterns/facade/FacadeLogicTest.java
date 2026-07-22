package io.kiponos.examples.patterns.facade;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FacadeLogicTest {
    @Test void truthy() {
        assertTrue(FacadeLiveKnobsApp.truthy("yes"));
        assertFalse(FacadeLiveKnobsApp.truthy("no"));
    }
}
