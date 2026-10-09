package com.minewatch.server;

import com.minewatch.entity.BotEntity;
import com.minewatch.entity.ModEntities;
import com.minewatch.server.BotStats.Difficulty;
import com.minewatch.server.BotStats.Type;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/** 매치 중 봇 생성/리스폰/정리. */
public final class BotManager {
    public static final int RESPAWN_TICKS = 100;

    private record Pending(int team, Type type, Difficulty diff, int at) {}
    private static final List<BotEntity> BOTS = new ArrayList<>();
    private static final List<Pending> PENDING = new ArrayList<>();

    public static List<BotEntity> all() { return BOTS; }

    public static BotEntity spawn(MinecraftServer server, int team, Type type, Difficulty diff) {
        MapData map = MapData.get();
        ServerWorld w = map.world(server);
        if (w == null || map.spawns(team).isEmpty()) return null;
        BotEntity b = ModEntities.BOT.create(w);
        if (b == null) return null;
        List<double[]> list = map.spawns(team);
        double[] s = list.get(w.getRandom().nextInt(list.size()));
        b.refreshPositionAndAngles(s[0] + (w.getRandom().nextDouble() - 0.5) * 2, s[1], s[2] + (w.getRandom().nextDouble() - 0.5) * 2, (float) s[3], 0f);
        b.setup(type, team, diff);
        w.spawnEntity(b);
        BOTS.add(b);
        return b;
    }

    /** AI 대전 시작 시: 적 팀(B)을 봇으로 채우고, 아군(A)은 인원이 모자란 만큼 아군 봇으로 채운다. */
    public static void spawnMatchBots(MinecraftServer server, Difficulty diff, int humans) {
        int n = diff.botsPerSide;
        for (int i = 0; i < n; i++) spawn(server, 1, BotStats.enemyTypeAt(i), diff);
        for (int i = 0; i < Math.max(0, n - humans); i++) spawn(server, 0, Type.FRIENDLY, diff);
    }

    /** 각 팀을 size 명까지 봇으로 채운다(A팀은 아군 봇, B팀은 적 봇 구성). humansA/B 는 이미 있는 인원. */
    public static void fillTeams(MinecraftServer server, Difficulty diff, int size, int humansA, int humansB) {
        for (int i = 0; i < size - humansA; i++) spawn(server, 0, Type.FRIENDLY, diff);
        for (int i = 0; i < size - humansB; i++) spawn(server, 1, BotStats.enemyTypeAt(i), diff);
    }

    public static void onBotDeath(BotEntity b) {
        MinecraftServer server = b.getServer();
        if (server == null || MatchManager.state() != MatchManager.LIVE) return;
        PENDING.add(new Pending(b.botTeam(), b.botType(), b.difficulty(), server.getTicks() + RESPAWN_TICKS));
    }

    public static void tick(MinecraftServer server) {
        if (BOTS.isEmpty() && PENDING.isEmpty()) return;
        BOTS.removeIf(BotEntity::isRemoved);
        if (MatchManager.state() != MatchManager.LIVE) return;
        int now = server.getTicks();
        for (int i = PENDING.size() - 1; i >= 0; i--) {
            Pending p = PENDING.get(i);
            if (now >= p.at()) { PENDING.remove(i); spawn(server, p.team(), p.type(), p.diff()); }
        }
    }

    /** 서버 종료 시: 엔티티는 월드와 함께 사라지므로 목록만 비운다. */
    public static void reset() { BOTS.clear(); PENDING.clear(); }

    public static void clearAll() {
        for (BotEntity b : BOTS) if (!b.isRemoved()) b.discard();
        BOTS.clear();
        PENDING.clear();
    }
    private BotManager() {}
}
