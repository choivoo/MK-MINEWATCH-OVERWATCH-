package com.minewatch.server;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.minewatch.hero.Hero;
import com.minewatch.hero.HeroRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** /minewatch 명령어 모음. */
public final class Commands {
    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(literal("minewatch")
                .then(heroCommand())
                .then(literal("bot").requires(s -> s.hasPermissionLevel(2))
                        .then(literal("clear").executes(c -> { BotManager.clearAll(); return 1; }))
                        .then(literal("spawn")
                                .then(argument("type", StringArgumentType.word()).suggests((c, b) -> {
                                    for (BotStats.Type t : BotStats.Type.values()) b.suggest(t.key);
                                    return b.buildFuture();
                                }).then(argument("team", IntegerArgumentType.integer(0, 1)).executes(c -> {
                                    BotStats.Type type = null;
                                    String key = StringArgumentType.getString(c, "type");
                                    for (BotStats.Type t : BotStats.Type.values()) if (t.key.equals(key)) type = t;
                                    if (type == null) { c.getSource().sendError(Text.literal("알 수 없는 봇 종류입니다.")); return 0; }
                                    if (BotManager.spawn(c.getSource().getServer(), IntegerArgumentType.getInteger(c, "team"), type,
                                            BotStats.Difficulty.NORMAL) == null) {
                                        c.getSource().sendError(Text.literal("봇을 만들 수 없습니다. 먼저 /minewatch map build 로 맵을 지으세요."));
                                        return 0;
                                    }
                                    return 1;
                                })))))
                .then(literal("team")
                        .then(literal("auto").executes(c -> team(c.getSource(), -1)))
                        .then(literal("a").executes(c -> team(c.getSource(), 0)))
                        .then(literal("b").executes(c -> team(c.getSource(), 1))))
                .then(literal("healthpack").requires(s -> s.hasPermissionLevel(2))
                        .then(literal("large").executes(c -> { var s = c.getSource(); HealthPacks.place(worldOf(s), s.getPosition(), true); return 1; }))
                        .then(literal("small").executes(c -> { var s = c.getSource(); HealthPacks.place(worldOf(s), s.getPosition(), false); return 1; }))
                        .then(literal("clear").executes(c -> HealthPacks.removeAll())))
                .then(literal("match").requires(s -> s.hasPermissionLevel(2))
                        .then(literal("stop").executes(c -> { MatchManager.stop(c.getSource().getServer()); return 1; }))
                        .then(literal("start")
                                .then(literal("tdm").executes(c -> startMatch(c.getSource(), MatchManager.MODE_TDM, 20, 300))
                                        .then(argument("kills", IntegerArgumentType.integer(1, 200)).executes(c ->
                                                startMatch(c.getSource(), MatchManager.MODE_TDM, IntegerArgumentType.getInteger(c, "kills"), 300))))
                                .then(literal("control").executes(c -> startMatch(c.getSource(), MatchManager.MODE_CONTROL, 100, 600))
                                        .then(argument("points", IntegerArgumentType.integer(1, 1000)).executes(c ->
                                                startMatch(c.getSource(), MatchManager.MODE_CONTROL, IntegerArgumentType.getInteger(c, "points"), 600))))))
                .then(literal("map").requires(s -> s.hasPermissionLevel(2))
                        .then(literal("build").then(argument("map", StringArgumentType.word()).suggests((c, b) -> {
                            GameMaps.ALL.forEach(m -> b.suggest(m.id()));
                            return b.buildFuture();
                        }).executes(c -> {
                            GameMaps.Def def = GameMaps.byId(StringArgumentType.getString(c, "map"));
                            if (def == null) { c.getSource().sendError(Text.literal("알 수 없는 맵입니다.")); return 0; }
                            GameMaps.buildAndApply(c.getSource().getServer().getOverworld(), def);
                            c.getSource().sendFeedback(() -> Text.literal("맵을 지었습니다: " + def.name()), false);
                            return 1;
                        })))
                        .then(literal("spawn")
                                .then(literal("a").executes(c -> spawn(c.getSource(), 0)))
                                .then(literal("b").executes(c -> spawn(c.getSource(), 1))))
                        .then(literal("point").executes(c -> {
                            ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
                            MapData.setPoint(p.getServer(), p.getServerWorld(), p.getX(), p.getY(), p.getZ());
                            c.getSource().sendFeedback(() -> Text.literal("점령지를 현재 위치로 설정했습니다."), false);
                            return 1;
                        }))
                        .then(literal("reset").executes(c -> {
                            MapData.reset(c.getSource().getServer());
                            c.getSource().sendFeedback(() -> Text.literal("맵 설정을 초기화했습니다."), false);
                            return 1;
                        }))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> heroCommand() {
        return literal("hero")
                .then(literal("none").executes(c -> {
                    ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
                    HeroManager.select(p, null);
                    c.getSource().sendFeedback(() -> Text.literal("영웅 선택을 해제했습니다."), false);
                    return 1;
                }))
                .then(argument("hero", StringArgumentType.word()).suggests((c, b) -> {
                    HeroRegistry.ids().forEach(b::suggest);
                    return b.buildFuture();
                }).executes(c -> {
                    ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
                    Hero h = HeroRegistry.get(StringArgumentType.getString(c, "hero"));
                    if (h == null) { c.getSource().sendError(Text.literal("알 수 없는 영웅입니다.")); return 0; }
                    HeroManager.select(p, h);
                    c.getSource().sendFeedback(() -> Text.literal("영웅 선택: " + h.id), false);
                    return 1;
                }));
    }

    /** 콘솔 명령은 월드가 없을 수 있어 오버월드로 대신한다. */
    private static net.minecraft.server.world.ServerWorld worldOf(ServerCommandSource s) {
        return s.getWorld() != null ? s.getWorld() : s.getServer().getOverworld();
    }

    private static int team(ServerCommandSource s, int team) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        MatchManager.setPreference(s.getPlayerOrThrow(), team);
        return 1;
    }

    private static int spawn(ServerCommandSource s, int team) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity p = s.getPlayerOrThrow();
        MapData.get().dim = p.getServerWorld().getRegistryKey().getValue().toString();
        MapData.addSpawn(p.getServer(), team, p.getX(), p.getY(), p.getZ(), p.getYaw());
        s.sendFeedback(() -> Text.literal((team == 0 ? "A" : "B") + "팀 스폰을 추가했습니다."), false);
        return 1;
    }

    private static int startMatch(ServerCommandSource s, int mode, int target, int seconds) {
        String err = MatchManager.start(s.getServer(), mode, target, seconds);
        if (err != null) { s.sendError(Text.literal(err)); return 0; }
        return 1;
    }
    private Commands() {}
}
