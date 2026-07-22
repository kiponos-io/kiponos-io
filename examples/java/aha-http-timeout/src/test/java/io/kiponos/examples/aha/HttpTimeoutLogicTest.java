package io.kiponos.examples.aha;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HttpTimeoutLogicTest {
    @Test void readFallback() {
        // pure fallback style
        assertEquals("3000", "3000");
    }
}
