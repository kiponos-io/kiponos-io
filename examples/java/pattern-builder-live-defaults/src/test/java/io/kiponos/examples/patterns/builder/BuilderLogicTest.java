package io.kiponos.examples.patterns.builder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BuilderLogicTest {
    @Test void truthy() { assertTrue(BuilderLiveDefaultsApp.truthy("yes")); }
}
