package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev0916PmChatMuteAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("chat-mute", AgenticDev0916PmChatMuteApp.KEY);
        assertEquals("agentic-dev-0916-pm-chat-mute", AgenticDev0916PmChatMuteApp.FOLDER);
        assertEquals("none", AgenticDev0916PmChatMuteApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev0916PmChatMuteApp.decide(null);
        assertEquals("none", d.value());
        assertEquals("group_chat_sends_live", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev0916PmChatMuteApp.decide("none");
        assertTrue(d.proceed());
        assertEquals("group_chat_sends_live", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev0916PmChatMuteApp.decide("ops-late-bags");
        assertFalse(blocked.proceed());
        assertEquals("mute_sends_keep_session", blocked.action());
    }
}
