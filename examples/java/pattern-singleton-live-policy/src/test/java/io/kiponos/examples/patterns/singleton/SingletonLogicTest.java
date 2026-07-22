package io.kiponos.examples.patterns.singleton;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SingletonLogicTest {
    @Test void sameInstance() {
        assertSame(SingletonLivePolicyApp.get(), SingletonLivePolicyApp.get());
    }
}
