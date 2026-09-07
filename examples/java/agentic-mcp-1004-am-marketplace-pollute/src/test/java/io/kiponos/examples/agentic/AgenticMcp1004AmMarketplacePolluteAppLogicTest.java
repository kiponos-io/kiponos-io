package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMcp1004AmMarketplacePolluteAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("skill-trust", AgenticMcp1004AmMarketplacePolluteApp.KEY);
        assertEquals("agentic-mcp-1004-am-marketplace-pollute", AgenticMcp1004AmMarketplacePolluteApp.FOLDER);
        assertEquals("core,reviewed", AgenticMcp1004AmMarketplacePolluteApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMcp1004AmMarketplacePolluteApp.decide(null);
        assertEquals("core,reviewed", d.value());
        assertEquals("honor_trusted_skills", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMcp1004AmMarketplacePolluteApp.decide("core,reviewed");
        assertTrue(d.proceed());
        assertEquals("honor_trusted_skills", d.action());
    }

    @Test
    void gatedSample() {
        var dflt = AgenticMcp1004AmMarketplacePolluteApp.decide("core,reviewed");
        assertTrue(dflt.proceed());
    }
}
