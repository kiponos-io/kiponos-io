package io.kiponos.examples.patterns.decorator;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DecoratorLogicTest {

    @Test
    void parseChainSplitsAndNormalizes() {
        assertEquals(List.of("metrics", "retry", "cache"),
                DecoratorLiveChainApp.parseChain(" metrics,Retry, cache "));
        assertTrue(DecoratorLiveChainApp.parseChain("").isEmpty());
        assertTrue(DecoratorLiveChainApp.parseChain(null).isEmpty());
    }

    @Test
    void wrapMetricsAddsTrace() {
        var trace = new java.util.ArrayList<String>();
        var core = (java.util.function.UnaryOperator<DecoratorLiveChainApp.Request>) req ->
                new DecoratorLiveChainApp.Request(req.path(), "ok");
        var metrics = DecoratorLiveChainApp.wrap("metrics", core, trace, 2, 30);
        var out = metrics.apply(new DecoratorLiveChainApp.Request("/x", null));
        assertEquals("ok", out.body());
        assertTrue(trace.stream().anyMatch(s -> s.startsWith("metrics:elapsedMs=")));
    }

    @Test
    void wrapRetryRecordsAttempts() {
        var trace = new java.util.ArrayList<String>();
        var core = (java.util.function.UnaryOperator<DecoratorLiveChainApp.Request>) req ->
                new DecoratorLiveChainApp.Request(req.path(), "ok");
        var retry = DecoratorLiveChainApp.wrap("retry", core, trace, 2, 30);
        retry.apply(new DecoratorLiveChainApp.Request("/y", null));
        assertTrue(trace.stream().anyMatch(s -> s.contains("retry:attempts=")));
    }
}
