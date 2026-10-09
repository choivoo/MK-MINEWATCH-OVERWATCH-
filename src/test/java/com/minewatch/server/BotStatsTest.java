package com.minewatch.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minewatch.server.BotStats.Difficulty;
import com.minewatch.server.BotStats.Type;
import org.junit.jupiter.api.Test;

class BotStatsTest {
    @Test void hitChanceDropsWithDistanceAndRisesWithDifficulty() {
        double near = BotStats.hitChance(Type.BASIC, Difficulty.NORMAL, 1);
        double far = BotStats.hitChance(Type.BASIC, Difficulty.NORMAL, 16);
        assertTrue(near > far);
        assertEquals(0.55 * 0.6, far, 1e-9);
        assertTrue(BotStats.hitChance(Type.BASIC, Difficulty.HARD, 8) > BotStats.hitChance(Type.BASIC, Difficulty.EASY, 8));
    }

    @Test void hitChanceNeverExceedsAccuracyOrGoesNegative() {
        for (Difficulty d : Difficulty.values()) for (Type t : Type.values()) for (double dist = 0; dist < 200; dist += 7) {
            double c = BotStats.hitChance(t, d, dist);
            assertTrue(c > 0 && c <= d.accuracy + 1e-9);
        }
    }

    @Test void harderBotsFireFaster() {
        assertTrue(BotStats.fireInterval(Type.ASSAULT, Difficulty.HARD) < BotStats.fireInterval(Type.ASSAULT, Difficulty.EASY));
        for (Type t : Type.values()) assertTrue(BotStats.fireInterval(t, Difficulty.HARD) >= 2);
    }

    @Test void splashFallsOffAndStopsAtRadius() {
        assertEquals(40, BotStats.splashDamage(Type.ROCKET, 0), 1e-9);
        assertEquals(20, BotStats.splashDamage(Type.ROCKET, 3), 1e-9);
        assertEquals(0, BotStats.splashDamage(Type.ROCKET, 3.01), 1e-9);
        assertEquals(0, BotStats.splashDamage(Type.BASIC, 0), 1e-9);
    }

    @Test void enemyCompositionCycles() {
        assertEquals(Type.BASIC, BotStats.enemyTypeAt(0));
        assertEquals(Type.ASSAULT, BotStats.enemyTypeAt(1));
        assertEquals(Type.ROCKET, BotStats.enemyTypeAt(2));
        assertEquals(Type.BASIC, BotStats.enemyTypeAt(3));
    }

    @Test void ordinalLookupWrapsSafely() {
        assertEquals(Difficulty.EASY, Difficulty.of(0));
        assertEquals(Difficulty.HARD, Difficulty.of(2));
        assertEquals(Difficulty.EASY, Difficulty.of(3));
        assertEquals(Type.FRIENDLY, Type.of(-1));
    }
}
