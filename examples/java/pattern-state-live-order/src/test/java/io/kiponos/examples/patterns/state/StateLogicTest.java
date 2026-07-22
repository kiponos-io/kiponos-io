package io.kiponos.examples.patterns.state;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StateLogicTest {

    @Test
    void parseAllowedEdges() {
        Set<String> edges = StateLiveOrderApp.parseAllowed("draft>paid, paid > shipped");
        assertTrue(edges.contains("draft>paid"));
        assertTrue(edges.contains("paid>shipped"));
    }

    @Test
    void allowedFromListsNextStates() {
        Set<String> edges = StateLiveOrderApp.parseAllowed("draft>paid,draft>cancelled,paid>shipped");
        assertEquals(java.util.List.of("paid", "cancelled"), StateLiveOrderApp.allowedFrom(edges, "draft"));
    }
}
