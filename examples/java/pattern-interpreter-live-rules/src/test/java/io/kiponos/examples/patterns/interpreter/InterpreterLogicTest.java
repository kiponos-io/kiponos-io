package io.kiponos.examples.patterns.interpreter;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class InterpreterLogicTest {
    @Test void adminAllows() {
        // pure match helper
        assertTrue(InterpreterLiveRulesApp.matches("role=admin", Map.of("role", "admin")));
    }
}
