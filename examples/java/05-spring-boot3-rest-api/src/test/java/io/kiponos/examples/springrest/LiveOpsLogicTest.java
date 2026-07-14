package io.kiponos.examples.springrest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure logic tests — no Spring context, no live hub.
 */
class LiveOpsLogicTest {

    @Test
    void parseTimeoutMsAcceptsPositiveInts() {
        assertEquals(5000, LiveOpsConfig.parseTimeoutMs("5000"));
        assertEquals(1, LiveOpsConfig.parseTimeoutMs("1"));
        assertEquals(2500, LiveOpsConfig.parseTimeoutMs(" 2500 "));
    }

    @Test
    void parseTimeoutMsFallsBackOnBadInput() {
        assertEquals(LiveOpsConfig.DEFAULT_TIMEOUT_MS, LiveOpsConfig.parseTimeoutMs(null));
        assertEquals(LiveOpsConfig.DEFAULT_TIMEOUT_MS, LiveOpsConfig.parseTimeoutMs(""));
        assertEquals(LiveOpsConfig.DEFAULT_TIMEOUT_MS, LiveOpsConfig.parseTimeoutMs("   "));
        assertEquals(LiveOpsConfig.DEFAULT_TIMEOUT_MS, LiveOpsConfig.parseTimeoutMs("abc"));
        assertEquals(LiveOpsConfig.DEFAULT_TIMEOUT_MS, LiveOpsConfig.parseTimeoutMs("0"));
        assertEquals(LiveOpsConfig.DEFAULT_TIMEOUT_MS, LiveOpsConfig.parseTimeoutMs("-10"));
    }

    @Test
    void parseGreetingTrimsAndDefaults() {
        assertEquals("Hi there", LiveOpsConfig.parseGreeting("  Hi there  "));
        assertEquals(LiveOpsConfig.DEFAULT_GREETING, LiveOpsConfig.parseGreeting(null));
        assertEquals(LiveOpsConfig.DEFAULT_GREETING, LiveOpsConfig.parseGreeting(""));
        assertEquals(LiveOpsConfig.DEFAULT_GREETING, LiveOpsConfig.parseGreeting("   "));
    }
}
