package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticTravelGroupChatLiveAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("chat-mute", AgenticTravelGroupChatLiveApp.KEY);
        assertEquals("agentic-travel-group-chat-live", AgenticTravelGroupChatLiveApp.FOLDER);
        assertEquals("none", AgenticTravelGroupChatLiveApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticTravelGroupChatLiveApp.decide(null);
        assertEquals("none", d.value());
        assertEquals("group_chat_sends_live", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticTravelGroupChatLiveApp.decide("none");
        assertTrue(d.proceed());
        assertEquals("group_chat_sends_live", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticTravelGroupChatLiveApp.decide("ops-late-bags");
        assertFalse(blocked.proceed());
        assertEquals("mute_sends_keep_session", blocked.action());
    }
}
