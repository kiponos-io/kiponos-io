package io.kiponos.examples.multienv;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MultiEnvLogicTest {

    @Test
    void infersEnvLabelFromProfilePath() {
        assertEquals("dev", MultiEnvProfileApp.inferEnvLabel("['my-app']['v1.0.0']['dev']['base']"));
        assertEquals("staging", MultiEnvProfileApp.inferEnvLabel("['my-app']['v1.0.0']['staging']['base']"));
        assertEquals("prod", MultiEnvProfileApp.inferEnvLabel("['my-app']['v1.0.0']['prod']['base']"));
        assertEquals("dev", MultiEnvProfileApp.inferEnvLabel(null));
    }

    @Test
    void defaultApiBaseMatchesEnv() {
        assertEquals("https://api.dev.example.com", MultiEnvProfileApp.defaultApiBaseFor("dev"));
        assertEquals("https://api.staging.example.com", MultiEnvProfileApp.defaultApiBaseFor("staging"));
        assertEquals("https://api.example.com", MultiEnvProfileApp.defaultApiBaseFor("prod"));
    }
}
