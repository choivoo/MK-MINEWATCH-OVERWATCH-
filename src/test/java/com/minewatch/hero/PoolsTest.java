package com.minewatch.hero;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PoolsTest {
    private static Pools pools(double armor, double shield) {
        Pools p = new Pools();
        p.init(armor, shield);
        return p;
    }

    @Test void shieldAbsorbsFirst() {
        Pools p = pools(0, 100);
        assertEquals(0, p.absorb(60), 1e-9);
        assertEquals(40, p.shield, 1e-9);
    }

    @Test void overflowGoesToHealthWithoutArmor() {
        Pools p = pools(0, 50);
        assertEquals(30, p.absorb(80), 1e-9);
        assertEquals(0, p.shield, 1e-9);
    }

    @Test void armorReducesByFlatFive() {
        Pools p = pools(100, 0);
        assertEquals(0, p.absorb(30), 1e-9);
        assertEquals(100 - 25, p.armor, 1e-9);
    }

    @Test void armorReductionCappedAtHalf() {
        Pools p = pools(100, 0);
        p.absorb(6);                       // 6-5=1 < 3 -> 3 으로 보정
        assertEquals(97, p.armor, 1e-9);
    }

    @Test void armorOverflowSpillsToHealth() {
        Pools p = pools(10, 0);
        assertEquals(25, p.absorb(40), 1e-9);  // 40 -> 35 로 감소, 방어구 10 소진, 25 가 체력으로
    }

    @Test void shieldRegensAfterDelay() {
        Pools p = pools(0, 100);
        p.absorb(100);
        for (int i = 0; i < Pools.SHIELD_REGEN_DELAY_TICKS; i++) p.tick();
        assertEquals(0, p.shield, 1e-9);
        for (int i = 0; i < 20; i++) p.tick();
        assertEquals(20, p.shield, 1e-9);
    }

    @Test void damageResetsRegenDelay() {
        Pools p = pools(0, 100);
        p.absorb(10);
        for (int i = 0; i < 50; i++) p.tick();
        p.absorb(10);
        assertEquals(Pools.SHIELD_REGEN_DELAY_TICKS, p.shieldDelay);
    }
}
