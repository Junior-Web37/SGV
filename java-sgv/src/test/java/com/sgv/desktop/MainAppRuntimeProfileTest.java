package com.sgv.desktop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainAppRuntimeProfileTest {

    @Test
    void shouldInjectDefaultMysqlProfileWhenMissing() {
        String[] args = MainApp.resolveSpringStartupArgs(new String[0]);

        assertTrue(java.util.Arrays.asList(args).contains("--spring.profiles.active=mysql"));
    }

    @Test
    void shouldPreserveExplicitProfileWhenAlreadyProvided() {
        String[] args = MainApp.resolveSpringStartupArgs(new String[]{"--spring.profiles.active=prod"});

        assertEquals(1, args.length);
        assertEquals("--spring.profiles.active=prod", args[0]);
    }
}
