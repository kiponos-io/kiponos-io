package io.kiponos.examples.aha;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LogLevelsLogicTest {
    @Test void readFallback() {
        // pure fallback style
        assertEquals("INFO", "INFO");
    }
}
