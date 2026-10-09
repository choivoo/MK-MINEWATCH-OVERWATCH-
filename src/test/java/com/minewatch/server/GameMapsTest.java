package com.minewatch.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minewatch.server.GameMaps.Def;
import com.minewatch.server.GameMaps.Mat;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 월드 없이 메모리 격자에 맵을 지어, 스폰/점령지/경사로가 정상인지 검증한다. */
class GameMapsTest {
    private static Map<Long, Mat> build(Def d) {
        Map<Long, Mat> grid = new HashMap<>();
        d.build().accept(new GameMaps.B((x, y, z, m) -> grid.put(key(x, y, z), m)));
        return grid;
    }
    private static long key(int x, int y, int z) { return ((long) (x + 512) << 40) | ((long) (y + 512) << 20) | (z + 512); }
    private static Mat at(Map<Long, Mat> g, int x, int y, int z) { return g.getOrDefault(key(x, y, z), Mat.AIR); }

    private static void assertStandable(Map<Long, Mat> g, int x, int y, int z, String what) {
        assertTrue(at(g, x, y - 1, z).solid(), what + ": 발밑이 비어 있음 " + x + "," + z);
        assertEquals(Mat.AIR, at(g, x, y, z), what + ": 몸이 막힘");
        assertEquals(Mat.AIR, at(g, x, y + 1, z), what + ": 머리가 막힘");
    }

    @Test void mapIdsAreUnique() {
        assertEquals(GameMaps.ALL.size(), GameMaps.ALL.stream().map(Def::id).distinct().count());
        assertTrue(GameMaps.ALL.size() >= 4);
    }

    @Test void spawnsAreStandableAndOnOwnSide() {
        for (Def d : GameMaps.ALL) {
            Map<Long, Mat> g = build(d);
            for (double[] s : d.spawnA()) {
                assertStandable(g, (int) Math.floor(s[0]), 0, (int) Math.floor(s[1]), d.id() + " A");
                assertTrue(s[1] < 0, d.id() + ": A 스폰은 -z 쪽");
                assertTrue(Math.abs(s[0]) < d.hx() && Math.abs(s[1]) < d.hz(), d.id() + ": A 스폰이 맵 밖");
            }
            for (double[] s : d.spawnB()) {
                assertStandable(g, (int) Math.floor(s[0]), 0, (int) Math.floor(s[1]), d.id() + " B");
                assertTrue(s[1] > 0, d.id() + ": B 스폰은 +z 쪽");
                assertTrue(Math.abs(s[0]) < d.hx() && Math.abs(s[1]) < d.hz(), d.id() + ": B 스폰이 맵 밖");
            }
        }
    }

    @Test void controlPointIsStandableAtCenter() {
        for (Def d : GameMaps.ALL) {
            Map<Long, Mat> g = build(d);
            // 중앙은 평지(y=0) 또는 고지대(y=2)일 수 있다: 어느 쪽이든 서 있을 수 있어야 한다.
            boolean ok = false;
            for (int y = 0; y <= 2 && !ok; y++)
                ok = at(g, 0, y - 1, 0).solid() && at(g, 0, y, 0) == Mat.AIR && at(g, 0, y + 1, 0) == Mat.AIR;
            assertTrue(ok, d.id() + ": 점령지 중앙에 설 수 없음");
        }
    }

    @Test void spawnAreasHaveFloorBlueRed() {
        for (Def d : GameMaps.ALL) {
            Map<Long, Mat> g = build(d);
            assertEquals(Mat.BLUE, at(g, 0, -1, -d.hz() + 3), d.id() + ": A 패드");
            assertEquals(Mat.RED, at(g, 0, -1, d.hz() - 3), d.id() + ": B 패드");
        }
    }

    /** 경사로의 마지막 계단이 높은 구조물과 맞닿아 있어야 한다(걸어서 올라갈 수 있음). */
    @Test void canyonRampsReachPlateau() {
        Map<Long, Mat> g = build(GameMaps.byId("canyon"));
        // 고지대 윗면은 y=2 에 서는 높이: 블록 y=0..1 이 채워져 있고 그 위는 공기
        assertTrue(at(g, 0, 1, -7).solid());
        assertEquals(Mat.STAIRS_N, at(g, 0, 0, -9));
        assertEquals(Mat.STAIRS_N, at(g, 0, 1, -8));      // 마지막 계단이 고지대 바로 앞(-7)
        assertEquals(Mat.STAIRS_S, at(g, 0, 1, 8));
    }

    @Test void towerRampsReachTowerTops() {
        Map<Long, Mat> g = build(GameMaps.byId("towers"));
        // 탑 x∈[-17,-13], 높이 6(블록 y 0..5). 경사로 마지막 계단은 y=5, x=-12.
        assertEquals(Mat.STAIRS_E, at(g, -12, 5, -8));
        assertTrue(at(g, -13, 5, -8).solid());
        assertEquals(Mat.STAIRS_W, at(g, 12, 5, -8));
        assertTrue(at(g, 13, 5, 8).solid());
    }

    @Test void nothingBuiltOutsideBounds() {
        for (Def d : GameMaps.ALL) {
            for (var e : build(d).entrySet()) {
                long k = e.getKey();
                int x = (int) (k >> 40) - 512, z = (int) (k & 0xFFFFF) - 512;
                assertTrue(Math.abs(x) <= d.hx() && Math.abs(z) <= d.hz(), d.id() + ": 범위 밖 블록 " + x + "," + z);
            }
        }
    }
}
