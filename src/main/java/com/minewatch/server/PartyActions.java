package com.minewatch.server;

import com.minewatch.net.PartyActionPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** 로비 화면(클라이언트)에서 온 동작 처리. 게임 시작/중지는 호스트 또는 OP 만 가능. */
public final class PartyActions {
    public static boolean canManage(ServerPlayerEntity p) {
        return p.hasPermissionLevel(2) || p.getServer().isHost(p.getGameProfile());
    }

    public static void handle(ServerPlayerEntity p, PartyActionPayload a) {
        if (a.action().equals("team")) { MatchManager.setPreference(p, a.team()); return; }
        if (!canManage(p)) { p.sendMessage(Text.literal("호스트 또는 OP 만 사용할 수 있습니다."), true); return; }
        switch (a.action()) {
            case "play" -> play(p, a.mode(), a.target(), false, 1);
            case "play_ai" -> play(p, a.mode(), a.target(), true, a.difficulty());
            case "stop" -> MatchManager.stop(p.getServer());
            default -> {}
        }
    }

    /** 내장 맵 중 하나를 랜덤으로 골라 짓고, 그 맵에서 매치를 시작한다. */
    private static void play(ServerPlayerEntity p, int mode, int target, boolean ai, int difficulty) {
        if (MatchManager.active()) { p.sendMessage(Text.literal("이미 매치가 진행 중입니다."), true); return; }
        GameMaps.Def def = GameMaps.pickRandom(p.getRandom());
        GameMaps.buildAndApply(p.getServer().getOverworld(), def);
        p.getServer().getPlayerManager().broadcast(Text.literal("맵: " + def.name()), false);
        int seconds = mode == MatchManager.MODE_CONTROL ? 600 : 300;
        String err = MatchManager.start(p.getServer(), mode, Math.max(1, target), seconds, ai, difficulty);
        if (err != null) p.sendMessage(Text.literal(err), true);
    }
    private PartyActions() {}
}
