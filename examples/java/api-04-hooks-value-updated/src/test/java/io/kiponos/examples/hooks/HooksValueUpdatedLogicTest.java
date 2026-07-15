package io.kiponos.examples.hooks;

import io.kiponos.sdk.data.ConfigValUpdatedResponse;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class HooksValueUpdatedLogicTest {

    @Test
    void parseMaxRpsAcceptsPositiveInts() {
        assertEquals(1, HooksValueUpdatedApp.parseMaxRps("1"));
        assertEquals(120, HooksValueUpdatedApp.parseMaxRps("120"));
        assertEquals(50, HooksValueUpdatedApp.parseMaxRps("50"));
    }

    @Test
    void parseMaxRpsFallsBackOnBadInput() {
        assertEquals(HooksValueUpdatedApp.DEFAULT_MAX_RPS, HooksValueUpdatedApp.parseMaxRps(null));
        assertEquals(HooksValueUpdatedApp.DEFAULT_MAX_RPS, HooksValueUpdatedApp.parseMaxRps(""));
        assertEquals(HooksValueUpdatedApp.DEFAULT_MAX_RPS, HooksValueUpdatedApp.parseMaxRps("  "));
        assertEquals(HooksValueUpdatedApp.DEFAULT_MAX_RPS, HooksValueUpdatedApp.parseMaxRps("nope"));
        assertEquals(HooksValueUpdatedApp.DEFAULT_MAX_RPS, HooksValueUpdatedApp.parseMaxRps("0"));
        assertEquals(HooksValueUpdatedApp.DEFAULT_MAX_RPS, HooksValueUpdatedApp.parseMaxRps("-3"));
    }

    @Test
    void isMaxRpsKeyIsCaseInsensitive() {
        assertTrue(HooksValueUpdatedApp.isMaxRpsKey("max-rps"));
        assertTrue(HooksValueUpdatedApp.isMaxRpsKey("MAX-RPS"));
        assertTrue(HooksValueUpdatedApp.isMaxRpsKey(" Max-Rps "));
        assertFalse(HooksValueUpdatedApp.isMaxRpsKey("min-rps"));
        assertFalse(HooksValueUpdatedApp.isMaxRpsKey(null));
        assertFalse(HooksValueUpdatedApp.isMaxRpsKey(""));
    }

    @Test
    void onValueUpdatedAppliesMatchingKeyAndIgnoresOthers() throws Exception {
        AtomicInteger live = new AtomicInteger(50);
        CountDownLatch latch = new CountDownLatch(1);

        HooksValueUpdatedApp.onValueUpdated(
                ConfigValUpdatedResponse.builder()
                        .key("greeting")
                        .value("hello")
                        .basePath("examples/hooks-value-updated")
                        .build(),
                live,
                latch
        );
        assertEquals(50, live.get());
        assertEquals(1, latch.getCount());

        HooksValueUpdatedApp.onValueUpdated(
                ConfigValUpdatedResponse.builder()
                        .key("max-rps")
                        .value("200")
                        .basePath("examples/hooks-value-updated")
                        .build(),
                live,
                latch
        );
        assertEquals(200, live.get());
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    void onValueUpdatedNullEventIsNoop() {
        AtomicInteger live = new AtomicInteger(10);
        CountDownLatch latch = new CountDownLatch(1);
        HooksValueUpdatedApp.onValueUpdated(null, live, latch);
        assertEquals(10, live.get());
        assertEquals(1, latch.getCount());
    }
}
