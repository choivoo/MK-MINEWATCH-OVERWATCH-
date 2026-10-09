package com.minewatch.server;

import com.minewatch.net.KillFeedPayload;
import com.minewatch.net.MatchPayload;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.joml.Vector3f;

/** 서버당 하나의 매치. 모드: 팀 데스매치 / 점령. 상태: 대기 -> 카운트다운 -> 진행 -> 종료. */
public final class MatchManager {
    public static final int NONE = 0, COUNTDOWN = 1, LIVE = 2, ENDED = 3;
    public static final int MODE_TDM = 0, MODE_CONTROL = 1;
    public static final int COUNTDOWN_TICKS = 200, ENDED_TICKS = 200, CAPTURE_TICKS = 100;

    private static int state = NONE, mode = MODE_TDM;
    private static boolean aiMatch = false, onlineMatch = false;
    private static int ticksLeft, target, timeLimit;
    private static final int[] score = new int[2];
    private static final Map<UUID, Integer> teams = new HashMap<>();
    /** 플레이어가 고른 팀 선호(0 A, 1 B). 없으면 자동 배정. */
    private static final Map<UUID, Integer> prefs = new HashMap<>();

    /** 매치 시작 전 위치(로비). 매치가 끝나면 여기로 돌려보낸다. */
    private record Origin(net.minecraft.registry.RegistryKey<net.minecraft.world.World> world, double x, double y, double z, float yaw, float pitch) {}
    private static final Map<UUID, Origin> origins = new HashMap<>();

    private static void recordOrigin(ServerPlayerEntity p) {
        if (origins.putIfAbsent(p.getUuid(), new Origin(p.getServerWorld().getRegistryKey(), p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch())) == null)
            persistOrigins(p.getServer());
    }

    private static java.nio.file.Path originsFile(MinecraftServer server) {
        return server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("minewatch_origins.json");
    }

    /** 매치 전 위치를 월드 폴더에 저장한다(서버가 매치 도중 꺼져도 복구할 수 있게). */
    static void persistOrigins(MinecraftServer server) {
        java.util.Map<String, OriginStore.Entry> m = new java.util.LinkedHashMap<>();
        for (var e : origins.entrySet()) {
            Origin o = e.getValue();
            m.put(e.getKey().toString(), new OriginStore.Entry(o.world().getValue().toString(), o.x(), o.y(), o.z(), o.yaw(), o.pitch()));
        }
        OriginStore.save(originsFile(server), m);
    }

    /** 서버 시작 시 저장된 위치 기록을 읽어 온다. */
    public static void loadOrigins(MinecraftServer server) {
        origins.clear();
        for (var e : OriginStore.load(originsFile(server)).entrySet()) {
            try {
                var o = e.getValue();
                origins.put(UUID.fromString(e.getKey()), new Origin(net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD,
                        net.minecraft.util.Identifier.of(o.world())), o.x(), o.y(), o.z(), o.yaw(), o.pitch()));
            } catch (RuntimeException ex) { /* 손상된 항목은 무시 */ }
        }
    }

    private static void returnToOrigins(MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            Origin o = origins.get(p.getUuid());
            if (o == null) continue;
            ServerWorld w = server.getWorld(o.world());
            if (w != null) p.teleport(w, o.x(), o.y(), o.z(), o.yaw(), o.pitch());
            origins.remove(p.getUuid());
            if (!p.isAlive()) continue;
            p.setHealth(p.getMaxHealth());
        }
        // 오프라인인 참가자의 위치 기록은 남겨 두었다가 재접속 때 로비로 돌려보낸다.
        persistOrigins(server);
    }

    // 점령지 상태
    private static final ControlPoint POINT = new ControlPoint();

    public static int state() { return state; }
    public static boolean active() { return state != NONE; }
    /** 영웅 변경은 매치가 진행(LIVE) 중이 아닐 때만 허용. */
    public static boolean canChangeHero() { return state != LIVE; }
    /** 카운트다운/종료 중에는 매치 참가자의 영웅 입력을 막는다. */
    public static boolean inputLocked(ServerPlayerEntity p) {
        return (state == COUNTDOWN || state == ENDED) && teamOf(p) >= 0;
    }
    public static int teamOf(ServerPlayerEntity p) { return teams.getOrDefault(p.getUuid(), -1); }
    public static int mode() { return mode; }
    /** 플레이어 또는 봇의 팀. 매치 참가자가 아니면 -1. */
    public static int teamOfEntity(net.minecraft.entity.Entity e) {
        if (e instanceof ServerPlayerEntity p) return teamOf(p);
        if (e instanceof com.minewatch.entity.BotEntity b) return b.botTeam();
        return -1;
    }
    public static boolean sameTeam(net.minecraft.entity.Entity a, net.minecraft.entity.Entity b) {
        int ta = teamOfEntity(a);
        return ta >= 0 && ta == teamOfEntity(b);
    }

    public static void setPreference(ServerPlayerEntity p, int team) {
        if (team < 0) prefs.remove(p.getUuid()); else prefs.put(p.getUuid(), team);
        p.sendMessage(Text.literal("팀 선호: " + (team < 0 ? "자동" : team == 0 ? "A팀" : "B팀")), false);
    }

    /** 매치를 시작한다. 실패하면 사유를 반환, 성공하면 null. */
    public static String start(MinecraftServer server, int newMode, int targetValue, int seconds) {
        return start(server, newMode, targetValue, seconds, false, 1);
    }

    /** ai=true 면 모든 플레이어를 A팀에 두고 B팀(및 부족한 A팀)을 봇으로 채운다. */
    public static String start(MinecraftServer server, int newMode, int targetValue, int seconds, boolean ai, int difficulty) {
        return start(server, newMode, targetValue, seconds, ai, difficulty, null, 0);
    }

    /**
     * participants 가 null 이면 접속 중인 모든 플레이어, 아니면 해당 플레이어만 참가(온라인 큐 매치).
     * fillTeamSize > 0 이면 각 팀을 그 인원까지 봇으로 채운다.
     */
    public static String start(MinecraftServer server, int newMode, int targetValue, int seconds, boolean ai, int difficulty,
                               java.util.Collection<ServerPlayerEntity> participants, int fillTeamSize) {
        onlineMatch = participants != null;
        BotManager.clearAll();
        aiMatch = ai;
        MapData map = MapData.get();
        if (newMode == MODE_CONTROL && !map.hasPoint()) return "점령지가 없습니다. 아레나를 먼저 만드세요.";
        mode = newMode == MODE_CONTROL ? MODE_CONTROL : MODE_TDM;
        score[0] = score[1] = 0;
        POINT.reset();
        target = targetValue; timeLimit = seconds * 20;
        teams.clear();
        List<ServerPlayerEntity> list = participants != null ? new java.util.ArrayList<>(participants) : server.getPlayerManager().getPlayerList();
        for (ServerPlayerEntity p : list) { recordOrigin(p); assign(p); }
        ServerWorld mw = map.world(server);
        if (mw != null) mw.setTimeOfDay(6000);   // 항상 낮에 시작
        state = COUNTDOWN; ticksLeft = COUNTDOWN_TICKS;
        for (ServerPlayerEntity p : list) { p.setHealth(p.getMaxHealth()); teleportToSpawn(p); }
        if (ai) BotManager.spawnMatchBots(server, BotStats.Difficulty.of(difficulty), list.size());
        else if (fillTeamSize > 0) BotManager.fillTeams(server, BotStats.Difficulty.of(difficulty), fillTeamSize, countTeam(0), countTeam(1));
        String what = (ai ? "AI 대전 · " : "") + (mode == MODE_CONTROL ? "점령전 (목표 " + target + "점)" : "팀 데스매치 (목표 " + target + "킬)");
        broadcast(server, Text.literal("매치가 곧 시작됩니다: " + what + ". 영웅을 고르세요!"));
        sync(server);
        return null;
    }

    public static void stop(MinecraftServer server) {
        state = NONE; teams.clear();
        BotManager.clearAll();
        aiMatch = false; onlineMatch = false;
        returnToOrigins(server);
        sync(server);
    }

    /** 선호 팀이 있으면 그 팀, 없으면 인원이 적은 팀. */
    public static void assign(ServerPlayerEntity p) {
        int a = 0, b = 0;
        for (int t : teams.values()) { if (t == 0) a++; else b++; }
        Integer pref = prefs.get(p.getUuid());
        int t = aiMatch ? 0 : pref != null ? pref : (a <= b ? 0 : 1);
        teams.put(p.getUuid(), t);
        p.sendMessage(Text.literal("당신은 " + (t == 0 ? "A" : "B") + "팀입니다."), false);
    }
    public static int countTeam(int team) {
        int n = 0;
        for (int t : teams.values()) if (t == team) n++;
        return n;
    }

    /**
     * 접속 처리. 진행 중 매치에 팀이 남아 있는 재접속자는 같은 팀/영웅으로 스폰에 복귀하고,
     * 매치가 끝난 뒤 돌아온 플레이어는 매치 전 위치(로비)로 보낸다.
     */
    public static void onJoin(ServerPlayerEntity p) {
        if (!active()) {
            Origin o = origins.remove(p.getUuid());
            if (o != null) persistOrigins(p.getServer());
            if (o != null) {
                ServerWorld w = p.getServer().getWorld(o.world());
                if (w != null) p.teleport(w, o.x(), o.y(), o.z(), o.yaw(), o.pitch());
            }
            return;
        }
        boolean rejoin = teams.containsKey(p.getUuid());
        if (!rejoin) {
            if (onlineMatch) return;           // 큐 매치 도중 새로 들어온 사람은 로비에 머문다
            recordOrigin(p);
            assign(p);
        }
        teleportToSpawn(p);
        if (rejoin) {
            var hero = HeroManager.last(p.getUuid());
            if (hero != null) HeroManager.select(p, hero);
            p.sendMessage(Text.literal("매치에 다시 참가했습니다."), false);
        }
    }
    /** 매치 중에는 팀 배정을 유지해 재접속할 수 있게 한다. */
    public static void onLeave(ServerPlayerEntity p) { if (!active()) teams.remove(p.getUuid()); }
    public static void onRespawn(ServerPlayerEntity p) { if (active()) teleportToSpawn(p); }

    /** 팀 스폰 지점 중 하나로 이동. 스폰이 없으면 아무것도 하지 않는다. */
    public static void teleportToSpawn(ServerPlayerEntity p) {
        int t = teamOf(p);
        MapData map = MapData.get();
        if (t < 0 || map.spawns(t).isEmpty()) return;
        ServerWorld w = map.world(p.getServer());
        if (w == null) return;
        List<double[]> list = map.spawns(t);
        double[] s = list.get(p.getRandom().nextInt(list.size()));
        p.teleport(w, s[0], s[1], s[2], (float) s[3], 0f);
    }

    /** 플레이어 또는 봇이 죽었을 때. 킬피드와 데스매치 점수를 처리한다. */
    public static void onDeath(net.minecraft.entity.LivingEntity victim, DamageSource src) {
        if (state != LIVE) return;
        int vt = teamOfEntity(victim);
        if (vt < 0) return;
        net.minecraft.entity.LivingEntity killer = src.getAttacker() instanceof net.minecraft.entity.LivingEntity k ? k : null;
        int kt = killer == null ? -1 : teamOfEntity(killer);
        MinecraftServer server = victim.getServer();
        KillFeedPayload kf = new KillFeedPayload(killer == null ? "" : killer.getName().getString(), kt,
                victim.getName().getString(), vt);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) ServerPlayNetworking.send(p, kf);
        if (mode == MODE_TDM && killer != null && killer != victim && kt >= 0 && kt != vt) score[kt]++;
    }

    public static void tick(MinecraftServer server) {
        if (state == NONE) return;
        if (state == COUNTDOWN) freezePlayers(server);
        if (state == LIVE && mode == MODE_CONTROL) controlTick(server);
        if (--ticksLeft <= 0) {
            switch (state) {
                case COUNTDOWN -> { state = LIVE; ticksLeft = timeLimit; broadcast(server, Text.literal("매치 시작!")); }
                case LIVE -> end(server);
                default -> stop(server);
            }
        } else if (state == LIVE && (score[0] >= target || score[1] >= target)) end(server);
        if (server.getTicks() % 10 == 0) sync(server);
    }

    private static void freezePlayers(MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (teamOf(p) < 0) continue;
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 5, 7, false, false, false));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 5, 128, false, false, false));
        }
    }

    private static void controlTick(MinecraftServer server) {
        MapData map = MapData.get();
        ServerWorld w = map.world(server);
        if (w == null || !map.hasPoint()) return;
        int[] present = new int[2];
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            int t = teamOf(p);
            if (t < 0 || !p.isAlive() || p.getServerWorld() != w) continue;
            double dx = p.getX() - map.point[0], dz = p.getZ() - map.point[2];
            if (dx * dx + dz * dz <= map.radius * map.radius && Math.abs(p.getY() - map.point[1]) <= 4) present[t]++;
        }
        int captured = POINT.tick(present[0], present[1]);
        if (captured >= 0) broadcast(server, Text.literal((captured == 0 ? "A" : "B") + "팀이 점령지를 차지했습니다!"));
        if (POINT.owner >= 0 && server.getTicks() % 20 == 0) score[POINT.owner]++;
        if (server.getTicks() % 10 == 0) {
            Vector3f col = POINT.owner == 0 ? new Vector3f(0.25f, 0.65f, 1f) : POINT.owner == 1 ? new Vector3f(1f, 0.3f, 0.3f)
                    : new Vector3f(0.85f, 0.85f, 0.85f);
            DustParticleEffect fx = new DustParticleEffect(col, 1.4f);
            for (int i = 0; i < 28; i++) {
                double a = i * Math.PI * 2 / 28;
                w.spawnParticles(fx, map.point[0] + Math.cos(a) * map.radius, map.point[1] + 0.3,
                        map.point[2] + Math.sin(a) * map.radius, 1, 0, 0, 0, 0);
            }
        }
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
                    : new MatchPayload(state, teamOf(p), score[0], score[1], target, ticksLeft,
                            mode, POINT.owner, POINT.progress, POINT.capTeam));
        }
    }

    public static void clear() { state = NONE; teams.clear(); prefs.clear(); origins.clear(); }
    private MatchManager() {}
}
