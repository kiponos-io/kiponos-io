package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticTravelGroupChatLiveAppLogicTest {
    @Test
    void hubKeyIsStable() {
        assertEquals("chat-mute", AgenticTravelGroupChatLiveApp.KEY);
        assertEquals("agentic-travel-group-chat-live", AgenticTravelGroupChatLiveApp.FOLDER);
        assertFalse(AgenticTravelGroupChatLiveApp.KEY.isBlank());
    }

    @Test
    void defaultIsNonEmpty() {
        assertFalse(AgenticTravelGroupChatLiveApp.DEFAULT.isBlank());
    }
}
