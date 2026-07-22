package io.kiponos.examples.patterns.command;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CommandLogicTest {
    @Test void truthy() { assertTrue(CommandLiveDispatchApp.truthy("yes")); }
}
