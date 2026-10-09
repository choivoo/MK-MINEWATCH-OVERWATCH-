package com.minewatch;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.minewatch.hero.Hero;
import com.minewatch.hero.HeroRegistry;
import com.minewatch.hero.Tracer;
import com.minewatch.hero.TracerState;
import com.minewatch.net.InputPayload;
import com.minewatch.net.StatePayload;
import com.minewatch.server.HeroManager;
import com.minewatch.server.MatchManager;
import com.minewatch.server.PulseBombs;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class MineWatch implements ModInitializer {
    public static final String ID = "minewatch";

    @Override
    public void onInitialize() {
        ModItems.init();
        HeroRegistry.init();

        PayloadTypeRegistry.playC2S().register(InputPayload.ID, InputPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StatePayload.ID, StatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(com.minewatch.net.SelectHeroPayload.ID, com.minewatch.net.SelectHeroPayload.CODEC);
        // TODO(M2): 매치 상태가 '대기/스폰 중'일 때만 변경 허용
        ServerPlayNetworking.registerGlobalReceiver(com.minewatch.net.SelectHeroPayload.ID,
                (payload, ctx) -> {
                    if (!MatchManager.canChangeHero()) { ctx.player().sendMessage(Text.literal("매치 진행 중에는 영웅을 바꿀 수 없습니다."), true); return; }
                    HeroManager.select(ctx.player(), HeroRegistry.get(payload.heroId()));
                });
        PayloadTypeRegistry.playS2C().register(com.minewatch.net.MatchPayload.ID, com.minewatch.net.MatchPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(com.minewatch.net.KillFeedPayload.ID, com.minewatch.net.KillFeedPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(InputPayload.ID,
                (payload, ctx) -> HeroManager.setInput(ctx.player(), payload));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            HeroManager.tickAll(server.getPlayerManager().getPlayerList());
            PulseBombs.tick();
            MatchManager.tick(server);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> { HeroManager.remove(h.player); MatchManager.onLeave(h.player); });
        ServerPlayConnectionEvents.JOIN.register((h, sender, s) -> MatchManager.onJoin(h.player));
        ServerLivingEntityEvents.AFTER_DEATH.register((e, src) -> { if (e instanceof ServerPlayerEntity p) MatchManager.onDeath(p, src); });
        // 같은 팀 아군 피해 차단
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((e, src, amt) ->
                !(e instanceof ServerPlayerEntity v && src.getAttacker() instanceof ServerPlayerEntity a && a != v && MatchManager.sameTeam(a, v)));
        ServerLifecycleCleanup.register();

        // 리콜 중에는 모든 피해에 무적
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity p && HeroManager.heroOf(p) instanceof Tracer
                    && ((TracerState) HeroManager.stateOf(p)).recalling) return false;
            return true;
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> dispatcher.register(
                literal("minewatch").then(literal("match").requires(s -> s.hasPermissionLevel(2))
                        .then(literal("stop").executes(c -> { MatchManager.stop(c.getSource().getServer()); return 1; }))
                        .then(literal("start").executes(c -> { MatchManager.start(c.getSource().getServer(), 20, 300); return 1; })
                                .then(argument("kills", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 200)).executes(c -> {
                                    MatchManager.start(c.getSource().getServer(), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "kills"), 300); return 1; }))))));
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> dispatcher.register(
                literal("minewatch").then(literal("hero")
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
                        })))));
    }
}
