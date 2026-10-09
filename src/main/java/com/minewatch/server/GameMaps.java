package com.minewatch.server;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * 내장 맵 모음. "게임 시작" 시 이 중 하나가 랜덤으로 뽑혀 월드의 고정 좌표에 (다시) 지어진다.
 * 좌표계: x/z 는 맵 중심 기준, y=0 은 걸어 다니는 바닥 위 공기층(바닥 블록은 y=-1).
 */
public final class GameMaps {
    public static final int FLOOR_Y = 120;

    /** 맵 정의. 스폰은 {x, z, yaw}. 팀 A 는 -z 쪽, B 는 +z 쪽. */
    public record Def(String id, String name, int index, Consumer<B> build,
                      double[][] spawnA, double[][] spawnB, double pointRadius) {
        public int baseX() { return 20000 + index * 300; }
        public int baseZ() { return 20000; }
    }

    /** 맵 건설 도우미. */
    public static final class B {
        final ServerWorld w; final int ox, oz;
        B(ServerWorld w, int ox, int oz) { this.w = w; this.ox = ox; this.oz = oz; }

        void set(int x, int y, int z, BlockState s) {
            w.setBlockState(new BlockPos(ox + x, FLOOR_Y + y, oz + z), s, Block.NOTIFY_LISTENERS);
        }
        void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
            for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
                for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                    for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, s);
        }
        void base(int hx, int hz, int height) {
            fill(-hx, 0, -hz, hx, height, hz, Blocks.AIR.getDefaultState());
            fill(-hx, -3, -hz, hx, -1, hz, Blocks.STONE_BRICKS.getDefaultState());
            // 외벽
            BlockState wall = Blocks.STONE_BRICKS.getDefaultState();
            fill(-hx, 0, -hz, hx, 6, -hz, wall); fill(-hx, 0, hz, hx, 6, hz, wall);
            fill(-hx, 0, -hz, -hx, 6, hz, wall); fill(hx, 0, -hz, hx, 6, hz, wall);
        }
        void pads(int hx, int hz) {
            fill(-4, -1, -hz + 1, 4, -1, -hz + 6, Blocks.BLUE_CONCRETE.getDefaultState());
            fill(-4, -1, hz - 6, 4, -1, hz - 1, Blocks.RED_CONCRETE.getDefaultState());
        }
        void pointRing(int cx, int cz, int r) {
            for (int dx = -r - 1; dx <= r + 1; dx++)
                for (int dz = -r - 1; dz <= r + 1; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d > r - 1 && d <= r) set(cx + dx, -1, cz + dz, Blocks.YELLOW_CONCRETE.getDefaultState());
                }
        }
        /** 계단 경사로: (x,z)에서 (dx,dz) 방향으로 올라가며 폭 3. */
        void ramp(int x, int z, int dx, int dz, int length, BlockState stairs) {
            Direction up = dz > 0 ? Direction.SOUTH : dz < 0 ? Direction.NORTH : dx > 0 ? Direction.EAST : Direction.WEST;
            BlockState s = stairs.with(StairsBlock.FACING, up.getOpposite());
            int px = dz != 0 ? 1 : 0, pz = dx != 0 ? 1 : 0;
            for (int i = 0; i < length; i++)
                for (int k = -1; k <= 1; k++) {
                    int cx = x + dx * i + px * k, cz = z + dz * i + pz * k;
                    if (i > 0) fill(cx, 0, cz, cx, i - 1, cz, Blocks.STONE_BRICKS.getDefaultState());
                    set(cx, i, cz, s);
                }
        }
        void block(int x1, int z1, int x2, int z2, int h, BlockState s) { fill(x1, 0, z1, x2, h - 1, z2, s); }
    }

    private static final BlockState BRICK = Blocks.STONE_BRICKS.getDefaultState();
    private static final BlockState DARK = Blocks.DEEPSLATE_BRICKS.getDefaultState();
    private static final BlockState STAIRS = Blocks.STONE_BRICK_STAIRS.getDefaultState();

    private static double[][] row(double z, double yaw, double... xs) {
        double[][] r = new double[xs.length][];
        for (int i = 0; i < xs.length; i++) r[i] = new double[]{xs[i] + 0.5, z, yaw};
        return r;
    }

    public static final List<Def> ALL = List.of(
            new Def("plaza", "중앙 광장", 0, b -> {
                b.base(20, 20, 10); b.pads(20, 20); b.pointRing(0, 0, 5);
                for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2)
                    b.block(sx * 9 - 1, sz * 9 - 1, sx * 9 + 1, sz * 9 + 1, 5, DARK);
                b.block(-6, -1, -4, 1, 2, BRICK); b.block(4, -1, 6, 1, 2, BRICK);
            }, row(-16.5, 0, -3, -1, 1, 3), row(17.5, 180, -3, -1, 1, 3), 5),

            new Def("canyon", "협곡", 1, b -> {
                b.base(14, 36, 12); b.pads(14, 36); b.pointRing(0, 0, 6);
                // 중앙 고지대(높이 2) + 남북 경사로
                b.fill(-7, 0, -7, 7, 1, 7, BRICK);
                b.ramp(0, -8, 0, 1, 2, STAIRS);
                b.ramp(0, 8, 0, -1, 2, STAIRS);
                for (int z = -24; z <= 24; z += 12) {
                    b.block(-11, z - 1, -8, z + 1, 3, DARK);
                    b.block(8, z + 3, 11, z + 5, 3, DARK);
                }
            }, row(-32.5, 0, -6, -2, 2, 6), row(33.5, 180, -6, -2, 2, 6), 6),

            new Def("fortress", "요새", 2, b -> {
                b.base(24, 24, 12); b.pads(24, 24); b.pointRing(0, 0, 5);
                // 중앙 요새 벽(두께 2, 높이 5) + 네 방향 출입구
                b.block(-10, -10, 10, -9, 5, DARK); b.block(-10, 9, 10, 10, 5, DARK);
                b.block(-10, -10, -9, 10, 5, DARK); b.block(9, -10, 10, 10, 5, DARK);
                BlockState air = Blocks.AIR.getDefaultState();
                b.fill(-2, 0, -10, 2, 4, -9, air); b.fill(-2, 0, 9, 2, 4, 10, air);
                b.fill(-10, 0, -2, -9, 4, 2, air); b.fill(9, 0, -2, 10, 4, 2, air);
                b.block(-15, -17, -11, -16, 3, BRICK); b.block(11, 16, 15, 17, 3, BRICK);
                b.block(-18, -4, -17, 4, 3, BRICK); b.block(17, -4, 18, 4, 3, BRICK);
            }, row(-20.5, 0, -3, -1, 1, 3), row(21.5, 180, -3, -1, 1, 3), 5),

            new Def("towers", "쌍탑", 3, b -> {
                b.base(26, 26, 14); b.pads(26, 26); b.pointRing(0, 0, 5);
                // 네 모서리 탑(5x5, 높이 6) + 안쪽에서 오르는 경사로
                int[][] towers = {{-15, -8}, {15, -8}, {-15, 8}, {15, 8}};
                for (int[] t : towers) {
                    b.fill(t[0] - 2, 0, t[1] - 2, t[0] + 2, 5, t[1] + 2, DARK);
                }
                b.ramp(-11, -8, 1, 0, 6, STAIRS);
                b.ramp(11, -8, -1, 0, 6, STAIRS);
                b.ramp(-11, 8, 1, 0, 6, STAIRS);
                b.ramp(11, 8, -1, 0, 6, STAIRS);
                b.block(-3, -13, 3, -12, 2, BRICK); b.block(-3, 12, 3, 13, 2, BRICK);
            }, row(-22.5, 0, -3, -1, 1, 3), row(23.5, 180, -3, -1, 1, 3), 5)
    );

    public static Def byId(String id) {
        for (Def d : ALL) if (d.id.equals(id)) return d;
        return null;
    }

    public static Def pickRandom(net.minecraft.util.math.random.Random r) { return ALL.get(r.nextInt(ALL.size())); }

    /** 맵을 지어 현재 맵으로 설정한다. */
    public static void buildAndApply(ServerWorld world, Def def) {
        def.build().accept(new B(world, def.baseX(), def.baseZ()));
        MapData m = new MapData();
        m.dim = world.getRegistryKey().getValue().toString();
        for (double[] s : def.spawnA()) m.spawnA.add(new double[]{def.baseX() + s[0], FLOOR_Y, def.baseZ() + s[1], s[2]});
        for (double[] s : def.spawnB()) m.spawnB.add(new double[]{def.baseX() + s[0], FLOOR_Y, def.baseZ() + s[1], s[2]});
        m.point = new double[]{def.baseX() + 0.5, FLOOR_Y, def.baseZ() + 0.5};
        m.radius = def.pointRadius();
        MapData.set(world.getServer(), m);
    }
    private GameMaps() {}
}
