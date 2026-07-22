package io.kiponos.examples.patterns.observer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ObserverLogicTest {
    @Test void csv() {
        assertEquals(java.util.List.of("metrics","audit"), ObserverLiveBusApp.csv(" metrics, Audit "));
    }
}
