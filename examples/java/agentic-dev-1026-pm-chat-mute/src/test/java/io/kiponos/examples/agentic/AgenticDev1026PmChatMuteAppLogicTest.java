package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticDev1026PmChatMuteAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("chat-mute", AgenticDev1026PmChatMuteApp.KEY);
        assertEquals("agentic-dev-1026-pm-chat-mute", AgenticDev1026PmChatMuteApp.FOLDER);
        assertEquals("none", AgenticDev1026PmChatMuteApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticDev1026PmChatMuteApp.decide(null);
        assertEquals("none", d.value());
        assertEquals("group_chat_sends_live", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticDev1026PmChatMuteApp.decide("none");
        assertTrue(d.proceed());
        assertEquals("group_chat_sends_live", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticDev1026PmChatMuteApp.decide("ops-late-bags");
        assertFalse(blocked.proceed());
        assertEquals("mute_sends_keep_session", blocked.action());
    }
}
