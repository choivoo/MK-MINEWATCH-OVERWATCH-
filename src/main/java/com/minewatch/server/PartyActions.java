package com.minewatch.server;

import com.minewatch.net.PartyActionPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** 파티 화면(클라이언트)에서 온 동작 처리. 매치 시작/중지/아레나 생성은 호스트 또는 OP 만 가능. */
public final class PartyActions {
    public static boolean canManage(ServerPlayerEntity p) {
        return p.hasPermissionLevel(2) || p.getServer().isHost(p.getGameProfile());
    }

    public static void handle(ServerPlayerEntity p, PartyActionPayload a) {
        if (a.action().equals("team")) { MatchManager.setPreference(p, a.team()); return; }
        if (!canManage(p)) { p.sendMessage(Text.literal("호스트 또는 OP 만 사용할 수 있습니다."), true); return; }
        switch (a.action()) {
            case "start" -> {
                int seconds = a.mode() == MatchManager.MODE_CONTROL ? 600 : 300;
                String err = MatchManager.start(p.getServer(), a.mode(), Math.max(1, a.target()), seconds);
                if (err != null) p.sendMessage(Text.literal(err), true);
            }
            case "stop" -> MatchManager.stop(p.getServer());
            case "arena" -> {
                MapData.buildArena(p.getServerWorld(), p.getBlockPos());
                p.sendMessage(Text.literal("아레나를 만들었습니다. 팀 스폰 4곳씩과 중앙 점령지가 설정되었습니다."), false);
            }
            default -> {}
        }
    }
    private PartyActions() {}
}
