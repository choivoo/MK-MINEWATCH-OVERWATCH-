package com.minewatch.server;

import com.minewatch.net.KillFeedPayload;
import com.minewatch.net.MatchPayload;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** 서버당 하나의 팀 데스매치. 상태: 대기 -> 카운트다운 -> 진행 -> 종료. */
public final class MatchManager {
    public static final int NONE = 0, COUNTDOWN = 1, LIVE = 2, ENDED = 3;
    public static final int COUNTDOWN_TICKS = 200, ENDED_TICKS = 200;

    private static int state = NONE;
    private static int ticksLeft, target, timeLimit;
    private static final int[] score = new int[2];
    private static final Map<UUID, Integer> teams = new HashMap<>();

    public static int state() { return state; }
    public static boolean active() { return state != NONE; }
    /** 영웅 변경은 매치가 진행(LIVE) 중이 아닐 때만 허용. */
    public static boolean canChangeHero() { return state != LIVE; }
    public static int teamOf(ServerPlayerEntity p) { return teams.getOrDefault(p.getUuid(), -1); }
    public static boolean sameTeam(ServerPlayerEntity a, ServerPlayerEntity b) {
        int ta = teamOf(a);
        return ta >= 0 && ta == teamOf(b);
    }

    public static void start(MinecraftServer server, int targetKills, int seconds) {
        score[0] = score[1] = 0;
        target = targetKills; timeLimit = seconds * 20;
        teams.clear();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) assign(p);
        state = COUNTDOWN; ticksLeft = COUNTDOWN_TICKS;
        broadcast(server, Text.literal("매치가 곧 시작됩니다. 영웅을 고르세요! (목표 " + target + "킬)"));
    }

    public static void stop(MinecraftServer server) {
        state = NONE; teams.clear();
        sync(server);
    }

    /** 인원이 적은 팀에 배정. */
    public static void assign(ServerPlayerEntity p) {
        int a = 0, b = 0;
        for (int t : teams.values()) { if (t == 0) a++; else b++; }
        int t = a <= b ? 0 : 1;
        teams.put(p.getUuid(), t);
        p.sendMessage(Text.literal("당신은 " + (t == 0 ? "A" : "B") + "팀입니다."), false);
    }
    public static void onJoin(ServerPlayerEntity p) { if (active()) assign(p); }
    public static void onLeave(ServerPlayerEntity p) { teams.remove(p.getUuid()); }

    public static void onDeath(ServerPlayerEntity victim, DamageSource src) {
        if (state != LIVE) return;
        int vt = teamOf(victim);
        if (vt < 0) return;
        ServerPlayerEntity killer = src.getAttacker() instanceof ServerPlayerEntity k ? k : null;
        int kt = killer == null ? -1 : teamOf(killer);
        MinecraftServer server = victim.getServer();
        KillFeedPayload kf = new KillFeedPayload(killer == null ? "" : killer.getName().getString(), kt,
                victim.getName().getString(), vt);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) ServerPlayNetworking.send(p, kf);
        if (killer != null && killer != victim && kt >= 0 && kt != vt) score[kt]++;
    }

    public static void tick(MinecraftServer server) {
        if (state == NONE) return;
        if (--ticksLeft <= 0) {
            switch (state) {
                case COUNTDOWN -> { state = LIVE; ticksLeft = timeLimit; broadcast(server, Text.literal("매치 시작!")); }
                case LIVE -> end(server);
                default -> stop(server);
            }
        } else if (state == LIVE && (score[0] >= target || score[1] >= target)) end(server);
        if (server.getTicks() % 10 == 0) sync(server);
    }

    private static void end(MinecraftServer server) {
        state = ENDED; ticksLeft = ENDED_TICKS;
        String r = score[0] == score[1] ? "무승부" : (score[0] > score[1] ? "A팀 승리" : "B팀 승리");
        broadcast(server, Text.literal("매치 종료: " + r + " (" + score[0] + " : " + score[1] + ")"));
        sync(server);
    }

    private static void broadcast(MinecraftServer server, Text t) { server.getPlayerManager().broadcast(t, false); }

    private static void sync(MinecraftServer server) {
        List<ServerPlayerEntity> list = server.getPlayerManager().getPlayerList();
        for (ServerPlayerEntity p : list) {
            ServerPlayNetworking.send(p, state == NONE ? MatchPayload.NONE
                    : new MatchPayload(state, teamOf(p), score[0], score[1], target, ticksLeft));
        }
    }

    public static void clear() { state = NONE; teams.clear(); }
    private MatchManager() {}
}
