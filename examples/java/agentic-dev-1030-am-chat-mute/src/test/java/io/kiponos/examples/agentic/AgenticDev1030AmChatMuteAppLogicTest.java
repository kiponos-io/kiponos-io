package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev1030AmChatMuteAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("chat-mute", AgenticDev1030AmChatMuteApp.KEY);
        assertEquals("agentic-dev-1030-am-chat-mute", AgenticDev1030AmChatMuteApp.FOLDER);
        assertEquals("none", AgenticDev1030AmChatMuteApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev1030AmChatMuteApp.decide(null);
        assertEquals("none", d.value());
        assertEquals("group_chat_sends_live", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev1030AmChatMuteApp.decide("none");
        assertTrue(d.proceed());
        assertEquals("group_chat_sends_live", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev1030AmChatMuteApp.decide("ops-late-bags");
        assertFalse(blocked.proceed());
        assertEquals("mute_sends_keep_session", blocked.action());
    }
}
