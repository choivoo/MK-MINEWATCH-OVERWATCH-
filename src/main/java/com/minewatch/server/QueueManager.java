package com.minewatch.server;

import com.minewatch.net.QueuePayload;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** 온라인 매치 큐: 최소 인원이 모이면 자동으로 랜덤 맵/모드의 매치를 시작한다. */
public final class QueueManager {
    private static QueueLogic queue = build();

    private static QueueLogic build() {
        ServerConfig c = ServerConfig.get();
        return new QueueLogic(c.minPlayers, c.maxPlayers, c.queueSeconds * 20);
    }

    /** 서버 시작 시 설정을 반영해 큐를 새로 만든다. */
    public static void init() { queue = build(); }
    public static void clear() { queue = build(); }

    public static void join(ServerPlayerEntity p) {
        if (queue.join(p.getUuid())) p.sendMessage(Text.literal("온라인 큐에 참가했습니다."), true);
        sendStatus(p);
    }

    public static void leave(ServerPlayerEntity p) {
        if (queue.leave(p.getUuid())) p.sendMessage(Text.literal("온라인 큐에서 나왔습니다."), true);
        sendStatus(p);
    }

    public static void onDisconnect(ServerPlayerEntity p) { queue.leave(p.getUuid()); }

    private static void sendStatus(ServerPlayerEntity p) {
        ServerPlayNetworking.send(p, new QueuePayload(queue.contains(p.getUuid()), queue.size(), queue.min(), queue.secondsLeft()));
    }

    public static void tick(MinecraftServer server) {
        List<UUID> picked = queue.tick(MatchManager.active());
        if (picked != null) startMatch(server, picked);
        if (server.getTicks() % 20 == 0) {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) if (queue.contains(p.getUuid())) sendStatus(p);
        }
    }

    private static void startMatch(MinecraftServer server, List<UUID> ids) {
        List<ServerPlayerEntity> players = new ArrayList<>();
        for (UUID id : ids) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
            if (p != null) players.add(p);
        }
        if (players.isEmpty()) return;
        ServerConfig c = ServerConfig.get();
        boolean control = c.mode.equals("control") || (c.mode.equals("random") && server.getOverworld().getRandom().nextBoolean());
        GameMaps.Def def = GameMaps.pickRandom(server.getOverworld().getRandom());
        GameMaps.buildAndApply(server.getOverworld(), def);
        server.getPlayerManager().broadcast(Text.literal("온라인 매치를 찾았습니다! 맵: " + def.name()), false);
        String err = MatchManager.start(server, control ? MatchManager.MODE_CONTROL : MatchManager.MODE_TDM,
                control ? c.targetPoints : c.targetKills, control ? 600 : 300, false, c.botDifficulty, players, c.fillTeamSize);
        if (err != null) for (ServerPlayerEntity p : players) p.sendMessage(Text.literal(err), true);
        for (ServerPlayerEntity p : players) sendStatus(p);
    }
    private QueueManager() {}
}
