package com.minewatch.test;

import com.minewatch.entity.BotEntity;
import com.minewatch.hero.Hero;
import com.minewatch.server.BotManager;
import com.minewatch.server.BotStats;
import com.minewatch.server.GameMaps;
import com.minewatch.server.MapData;
import com.minewatch.server.MatchManager;
import com.minewatch.server.OwDamage;
import java.util.ArrayList;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;

/** 실제 서버를 띄워 맵 건설, 봇 규칙, 봇 매치 진행을 검증한다. */
public class MineWatchGameTests implements FabricGameTest {

    private static void check(boolean ok, String msg) { if (!ok) throw new RuntimeException(msg); }

    private static void cleanup(MinecraftServer server) {
        MatchManager.stop(server);
        BotManager.clearAll();
        MapData.clear();
    }

    /** 맵 영역의 청크를 강제 로드해 엔티티/블록이 정상 동작하게 한다. */
    private static void forceLoad(ServerWorld w, GameMaps.Def d, boolean on) {
        int x0 = (d.baseX() - d.hx()) >> 4, x1 = (d.baseX() + d.hx()) >> 4;
        int z0 = (d.baseZ() - d.hz()) >> 4, z1 = (d.baseZ() + d.hz()) >> 4;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) w.setChunkForced(x, z, on);
    }

    @GameTest(batchId = "mw1", templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 400)
    public void mapsBuildIntoRealBlocks(TestContext ctx) {
        ServerWorld w = ctx.getWorld();
        for (GameMaps.Def d : GameMaps.ALL) {
            forceLoad(w, d, true);
            GameMaps.buildAndApply(w, d);
            MapData m = MapData.get();
            check(m.hasSpawns() && m.hasPoint(), d.id() + ": 스폰/점령지 미설정");
            for (int team = 0; team < 2; team++) {
                for (double[] s : m.spawns(team)) {
                    BlockPos feet = BlockPos.ofFloored(s[0], s[1], s[2]);
                    check(w.getBlockState(feet.down()).isSolidBlock(w, feet.down()), d.id() + ": 스폰 발밑이 비어 있음 " + feet);
                    check(w.getBlockState(feet).isAir() && w.getBlockState(feet.up()).isAir(), d.id() + ": 스폰이 막혀 있음 " + feet);
                }
            }
            forceLoad(w, d, false);
        }
        // 계단 방향: 협곡 남향 경사로(z 증가 방향으로 올라감)는 facing=NORTH 여야 한다.
        GameMaps.Def canyon = GameMaps.byId("canyon");
        forceLoad(w, canyon, true);
        GameMaps.buildAndApply(w, canyon);
        BlockState st = w.getBlockState(new BlockPos(canyon.baseX(), GameMaps.FLOOR_Y, canyon.baseZ() - 9));
        check(st.getBlock() instanceof StairsBlock, "협곡 경사로 첫 칸이 계단이 아님: " + st);
        check(st.get(StairsBlock.FACING) == Direction.NORTH, "계단 방향이 잘못됨: " + st.get(StairsBlock.FACING));
        forceLoad(w, canyon, false);
        cleanup(w.getServer());
        ctx.complete();
    }

    @GameTest(batchId = "mw2", templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 400)
    public void botsFollowTeamAndDamageRules(TestContext ctx) {
        ServerWorld w = ctx.getWorld();
        GameMaps.Def d = GameMaps.byId("plaza");
        forceLoad(w, d, true);
        GameMaps.buildAndApply(w, d);
        MinecraftServer server = w.getServer();

        BotEntity a = BotManager.spawn(server, 0, BotStats.Type.FRIENDLY, BotStats.Difficulty.NORMAL);
        BotEntity a2 = BotManager.spawn(server, 0, BotStats.Type.BASIC, BotStats.Difficulty.NORMAL);
        BotEntity b = BotManager.spawn(server, 1, BotStats.Type.ASSAULT, BotStats.Difficulty.NORMAL);
        check(a != null && a2 != null && b != null, "봇 생성 실패");

        check(Math.abs(b.getMaxHealth() - BotStats.Type.ASSAULT.health / Hero.HP_SCALE) < 0.01,
                "돌격 봇 체력이 250 OW 가 아님: " + b.getMaxHealth() * Hero.HP_SCALE);
        check(b.botTeam() == 1 && a.botTeam() == 0, "봇 팀이 잘못됨");

        float before = a2.getHealth();
        check(OwDamage.deal(a, a2, 40, false, false) == 0 && a2.getHealth() == before, "아군 봇이 피해를 입음");
        float bBefore = b.getHealth();
        double dealt = OwDamage.deal(a, b, 40, false, false);
        check(dealt > 0 && b.getHealth() < bBefore, "적 봇이 피해를 입지 않음");
        check(Math.abs(dealt - 40) < 0.5, "피해량이 40 OW 가 아님: " + dealt);

        forceLoad(w, d, false);
        cleanup(server);
        ctx.complete();
    }

    @GameTest(batchId = "mw3", templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 1800)
    public void botsFightEachOtherInMatch(TestContext ctx) {
        ServerWorld w = ctx.getWorld();
        MinecraftServer server = w.getServer();
        GameMaps.Def d = GameMaps.byId("plaza");
        forceLoad(w, d, true);
        GameMaps.buildAndApply(w, d);

        // 참가 플레이어 0명 + 팀당 3명 봇으로 채움(어려움)
        String err = MatchManager.start(server, MatchManager.MODE_TDM, 50, 300, false, 2, new ArrayList<>(), 3);
        check(err == null, "매치 시작 실패: " + err);
        check(MatchManager.state() == MatchManager.COUNTDOWN, "카운트다운 상태가 아님");
        check(BotManager.all().size() == 6, "봇이 6마리가 아님: " + BotManager.all().size());

        ctx.runAtTick(1500, () -> {
            check(MatchManager.state() == MatchManager.LIVE, "매치가 진행 상태가 아님: " + MatchManager.state());
            boolean damaged = false;
            for (BotEntity b : BotManager.all()) if (b.isRemoved() || b.getHealth() < b.getMaxHealth()) damaged = true;
            check(damaged, "1300틱 동안 봇들이 서로 전혀 싸우지 않음");
            forceLoad(w, d, false);
            cleanup(server);
            ctx.complete();
        });
    }

    @GameTest(batchId = "mw4", templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 600)
    public void onlineMatchTeamsSpawnLockAndRejoin(TestContext ctx) {
        ServerWorld w = ctx.getWorld();
        MinecraftServer server = w.getServer();
        GameMaps.Def d = GameMaps.byId("fortress");
        forceLoad(w, d, true);
        GameMaps.buildAndApply(w, d);

        net.minecraft.server.network.ServerPlayerEntity p = ctx.createMockCreativeServerPlayerInWorld();
        ArrayList<net.minecraft.server.network.ServerPlayerEntity> list = new ArrayList<>();
        list.add(p);
        String err = MatchManager.start(server, MatchManager.MODE_TDM, 10, 300, false, 1, list, 0);
        check(err == null, "매치 시작 실패: " + err);

        int team = MatchManager.teamOf(p);
        check(team == 0 || team == 1, "팀 배정이 안 됨");
        // 스폰 지점(맵 영역)으로 이동했는지
        check(Math.abs(p.getX() - d.baseX()) <= d.hx() && Math.abs(p.getZ() - d.baseZ()) <= d.hz(),
                "플레이어가 맵 스폰으로 이동하지 않음: " + p.getPos());
        // 카운트다운 중 영웅 입력 잠금
        check(MatchManager.inputLocked(p), "카운트다운 중 입력이 잠기지 않음");
        // 큐 매치 도중 새로 들어온 사람은 참가하지 않는다
        check(MatchManager.teamOfEntity(p) == team, "teamOfEntity 불일치");

        // 접속 종료 후에도 팀이 유지되어 같은 팀으로 재접속
        MatchManager.onLeave(p);
        check(MatchManager.teamOf(p) == team, "매치 중 접속 종료 시 팀이 사라짐");
        MatchManager.onJoin(p);
        check(MatchManager.teamOf(p) == team, "재접속 후 팀이 바뀜");

        forceLoad(w, d, false);
        cleanup(server);
        check(MatchManager.teamOf(p) == -1, "매치 종료 후 팀이 남아 있음");
        ctx.complete();
    }
}
