package io.kiponos.examples.patterns.bridge;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BridgeLogicTest {
    @Test void abstractionDelegates() {
        var n = new BridgeLiveImplementorApp.Notification(b -> "x:" + b);
        assertEquals("x:hi", n.send("hi"));
    }
}
