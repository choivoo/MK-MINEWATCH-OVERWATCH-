package com.minewatch.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ServerConfigTest {
    @Test void sanitizeClampsAbsurdValues() {
        ServerConfig c = new ServerConfig();
        c.minPlayers = -5; c.maxPlayers = 0; c.queueSeconds = 99999; c.fillTeamSize = 500;
        c.botDifficulty = 9; c.mode = "banana"; c.targetKills = 0; c.targetPoints = -1;
        c.sanitize();
        assertEquals(1, c.minPlayers);
        assertTrue(c.maxPlayers >= c.minPlayers);
        assertEquals(600, c.queueSeconds);
        assertEquals(16, c.fillTeamSize);
        assertEquals(2, c.botDifficulty);
        assertEquals("random", c.mode);
        assertEquals(1, c.targetKills);
        assertEquals(1, c.targetPoints);
    }

    @Test void defaultsAreValid() {
        ServerConfig c = new ServerConfig();
        c.sanitize();
        assertEquals(2, c.minPlayers);
        assertEquals(12, c.maxPlayers);
    }
}
