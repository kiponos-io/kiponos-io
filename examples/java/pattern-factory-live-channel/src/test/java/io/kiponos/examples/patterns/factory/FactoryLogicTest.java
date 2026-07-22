package io.kiponos.examples.patterns.factory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FactoryLogicTest {

    @Test
    void emailNotifierFormatsReceipt() {
        var n = new FactoryLiveChannelApp.EmailNotifier("ops@example.com");
        assertEquals("email", n.channel());
        assertTrue(n.send("hi").contains("ops@example.com"));
    }

    @Test
    void slackNotifierUsesHookLabel() {
        var n = new FactoryLiveChannelApp.SlackNotifier("#incidents");
        assertEquals("slack", n.channel());
        assertTrue(n.send("page").contains("#incidents"));
    }
}
