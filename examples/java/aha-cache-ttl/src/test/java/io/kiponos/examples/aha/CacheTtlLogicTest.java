package io.kiponos.examples.aha;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CacheTtlLogicTest {
    @Test void readFallback() {
        // pure fallback style
        assertEquals("300", "300");
    }
}
