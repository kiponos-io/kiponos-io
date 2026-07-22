package io.kiponos.examples.patterns.proxy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ProxyLogicTest {
    @Test void truthy() {
        assertTrue(ProxyLiveAccessApp.truthy("yes"));
        assertFalse(ProxyLiveAccessApp.truthy("no"));
    }
}
