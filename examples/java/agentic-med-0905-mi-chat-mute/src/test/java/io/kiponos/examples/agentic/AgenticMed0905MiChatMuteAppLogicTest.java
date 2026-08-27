package io.kiponos.examples.agentic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgenticMed0905MiChatMuteAppLogicTest {
    @Test
    void hubLeafIsStable() {
        assertEquals("chat-mute", AgenticMed0905MiChatMuteApp.KEY);
        assertEquals("agentic-med-0905-mi-chat-mute", AgenticMed0905MiChatMuteApp.FOLDER);
        assertEquals("none", AgenticMed0905MiChatMuteApp.DEFAULT);
    }

    @Test
    void defaultPathProceeds() {
        var d = AgenticMed0905MiChatMuteApp.decide(null);
        assertEquals("none", d.value());
        assertEquals("group_chat_sends_live", d.action());
        assertTrue(d.proceed());
    }

    @Test
    void liveSample() {
        var d = AgenticMed0905MiChatMuteApp.decide("none");
        assertTrue(d.proceed());
        assertEquals("group_chat_sends_live", d.action());
    }

    @Test
    void gatedSample() {
        var blocked = AgenticMed0905MiChatMuteApp.decide("ops-late-bags");
        assertFalse(blocked.proceed());
        assertEquals("mute_sends_keep_session", blocked.action());
    }
}
