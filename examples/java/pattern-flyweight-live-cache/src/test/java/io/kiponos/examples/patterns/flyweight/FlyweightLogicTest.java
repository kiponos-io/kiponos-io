package io.kiponos.examples.patterns.flyweight;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class FlyweightLogicTest {
    @Test void evicts() {
        Map<String,String> c = new LinkedHashMap<>();
        FlyweightLiveCacheApp.put(c, 2, "a", "1");
        FlyweightLiveCacheApp.put(c, 2, "b", "2");
        FlyweightLiveCacheApp.put(c, 2, "c", "3");
        assertEquals(2, c.size());
        assertFalse(c.containsKey("a"));
    }
}
