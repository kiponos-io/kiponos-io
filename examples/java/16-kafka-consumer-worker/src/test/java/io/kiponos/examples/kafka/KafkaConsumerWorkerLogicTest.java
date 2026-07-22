package io.kiponos.examples.kafka;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KafkaConsumerWorkerLogicTest {

    @Test
    void parseTruthiness() {
        assertTrue(KafkaConsumerWorkerApp.parseTruth("yes"));
        assertTrue(KafkaConsumerWorkerApp.parseTruth("PAUSED"));
        assertFalse(KafkaConsumerWorkerApp.parseTruth("no"));
        assertFalse(KafkaConsumerWorkerApp.parseTruth(null));
    }

    @Test
    void parsePositiveIntFallsBack() {
        assertEquals(100, KafkaConsumerWorkerApp.parsePositiveInt(null, 100));
        assertEquals(100, KafkaConsumerWorkerApp.parsePositiveInt("0", 100));
        assertEquals(100, KafkaConsumerWorkerApp.parsePositiveInt("x", 100));
        assertEquals(50, KafkaConsumerWorkerApp.parsePositiveInt("50", 100));
    }

    @Test
    void pausedSummary() {
        var p = KafkaConsumerWorkerApp.resolvePolicy(true, 500, 100);
        assertTrue(p.paused());
        assertTrue(p.summaryLine().contains("PAUSED"));
    }

    @Test
    void runningClampsPrefetchAtLeastMaxPoll() {
        var p = KafkaConsumerWorkerApp.resolvePolicy(false, 10, 100);
        assertEquals(100, p.prefetch());
        assertEquals(100, p.maxPollRecords());
        assertTrue(p.summaryLine().contains("RUNNING"));
    }
}
