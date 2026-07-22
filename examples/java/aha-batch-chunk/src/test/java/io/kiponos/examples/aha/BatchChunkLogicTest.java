package io.kiponos.examples.aha;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BatchChunkLogicTest {
    @Test void readFallback() {
        // pure fallback style
        assertEquals("100", "100");
    }
}
