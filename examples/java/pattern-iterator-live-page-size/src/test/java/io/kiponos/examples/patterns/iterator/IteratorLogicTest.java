package io.kiponos.examples.patterns.iterator;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class IteratorLogicTest {
    @Test void pages() {
        assertEquals(List.of("a","b"), IteratorLivePageSizeApp.pageOf(List.of("a","b","c"), 2, 0));
        assertEquals(List.of("c"), IteratorLivePageSizeApp.pageOf(List.of("a","b","c"), 2, 1));
    }
}
