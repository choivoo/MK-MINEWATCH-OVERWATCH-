package com.minewatch.hero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HeroMathTest {
    private static final GunSpec RIFLE = new GunSpec(30, 30, 20, 2, 30, 55, 0.5, 70, 9, 1);

    @Test void falloffIsLinearBetweenStartAndEnd() {
        assertEquals(20, RIFLE.damageAt(10, false), 1e-9);
        assertEquals(20, RIFLE.damageAt(30, false), 1e-9);
        assertEquals(15, RIFLE.damageAt(42.5, false), 1e-9);
        assertEquals(10, RIFLE.damageAt(55, false), 1e-9);
        assertEquals(10, RIFLE.damageAt(200, false), 1e-9);
        assertEquals(40, RIFLE.damageAt(5, true), 1e-9);
    }

    @Test void fireRateNeverExceedsSpec() {
        GunState g = new GunState(); g.fill(RIFLE);
        int total = 0;
        for (int t = 0; t < 20; t++) total += g.shots(RIFLE, true);
        assertTrue(total >= 9 && total <= 10, "1초에 " + total + "발");
    }

    @Test void idleAccumulationAllowsOnlyOneBurstShot() {
        GunState g = new GunState(); g.fill(RIFLE);
        for (int t = 0; t < 200; t++) g.shots(RIFLE, false);
        assertEquals(1, g.shots(RIFLE, true));
    }

    @Test void stopsWhenOutOfAmmoAndReloadsFully() {
        GunState g = new GunState(); g.fill(RIFLE);
        int total = 0;
        for (int t = 0; t < 400; t++) total += g.shots(RIFLE, true);
        assertEquals(30, total);
        g.startReload(RIFLE);
        for (int t = 0; t < RIFLE.reloadTicks() - 1; t++) assertFalse(g.tickReload(RIFLE));
        assertTrue(g.tickReload(RIFLE));
        assertEquals(30, g.ammo);
    }

    @Test void barrierAbsorbsOnlyFrontalDamageAndBreaks() {
        HeroState s = new HeroState();
        s.barrierUp = true; s.barrierHp = s.barrierMax = 100;
        // 방어자는 +z 를 바라봄. 공격자가 정면(+z)이면 흡수, 뒤(-z)이면 통과.
        assertEquals(0, s.absorbFront(0, 1, 0, 5, 70, 60), 1e-9);
        assertEquals(40, s.barrierHp, 1e-9);
        assertEquals(50, s.absorbFront(0, 1, 0, -5, 70, 50), 1e-9);
        assertEquals(40, s.barrierHp, 1e-9);
        assertEquals(20, s.absorbFront(0, 1, 1, 5, 70, 60), 1e-9);          // 남은 40 만 흡수
        assertFalse(s.barrierUp);
        assertTrue(s.barrierBroken > 0);
        assertEquals(30, s.absorbFront(0, 1, 0, 5, 70, 30), 1e-9);          // 파괴 후엔 흡수 안 함
    }

    @Test void sideAngleOutsideArcPassesThrough() {
        HeroState s = new HeroState();
        s.barrierUp = true; s.barrierHp = 500;
        assertEquals(40, s.absorbFront(0, 1, 5, 0.5, 70, 40), 1e-9);        // 거의 90도 옆
    }

    @Test void buffsApplyAndExpire() {
        HeroState s = new HeroState();
        assertEquals(1.0, s.outMult());
        s.boostOut(0.5, 3); s.resist(0.5, 2);
        assertEquals(1.5, s.outMult(), 1e-9);
        assertEquals(0.5, s.inMult(), 1e-9);
        s.tickEffects(); s.tickEffects();
        assertEquals(1.0, s.inMult(), 1e-9);
        assertEquals(1.5, s.outMult(), 1e-9);
        s.tickEffects();
        assertEquals(1.0, s.outMult(), 1e-9);
    }

    @Test void strongerBuffNotOverwrittenByWeaker() {
        HeroState s = new HeroState();
        s.boostOut(0.5, 100);
        s.boostOut(0.3, 10);
        assertEquals(1.5, s.outMult(), 1e-9);
    }

    @Test void cooldownsTickDown() {
        HeroState s = new HeroState();
        s.cd[0] = 2;
        s.tickEffects(); s.tickEffects(); s.tickEffects();
        assertEquals(0, s.cd[0]);
    }
}
