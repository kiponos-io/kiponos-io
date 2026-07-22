package io.kiponos.examples.patterns.memento;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class MementoLogicTest {
    @Test void retention() {
        Deque<String> d = new ArrayDeque<>();
        MementoLiveRetentionApp.push(d, 2, "a");
        MementoLiveRetentionApp.push(d, 2, "b");
        MementoLiveRetentionApp.push(d, 2, "c");
        assertEquals(2, d.size());
        assertEquals("b", d.peekFirst());
    }
}
