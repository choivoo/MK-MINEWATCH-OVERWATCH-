package com.minewatch;

import com.minewatch.hero.HeroRegistry;
import com.minewatch.net.DamageDirPayload;
import com.minewatch.net.HitPayload;
import com.minewatch.net.InputPayload;
import com.minewatch.net.KillFeedPayload;
import com.minewatch.net.MatchPayload;
import com.minewatch.net.PartyActionPayload;
import com.minewatch.net.PoolsPayload;
import com.minewatch.net.SelectHeroPayload;
import com.minewatch.net.StatePayload;
import com.minewatch.server.Commands;
import com.minewatch.server.HealthPacks;
import com.minewatch.server.HeroManager;
import com.minewatch.server.MapData;
import com.minewatch.server.MatchManager;
import com.minewatch.server.PartyActions;
import com.minewatch.server.PulseBombs;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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
        com.minewatch.server.ServerConfig.load();
        HeroRegistry.init();
        com.minewatch.entity.ModEntities.init();
        registerNetworking();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            HeroManager.tickAll(server.getPlayerManager().getPlayerList());
            PulseBombs.tick();
            MatchManager.tick(server);
            HealthPacks.tick(server);
            com.minewatch.server.BotManager.tick(server);
            com.minewatch.server.QueueManager.tick(server);
            com.minewatch.server.VenomMines.tick(server);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> { MapData.load(server); MatchManager.loadOrigins(server); com.minewatch.server.QueueManager.init(); });
        ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> { com.minewatch.server.QueueManager.onDisconnect(h.player); MatchManager.onLeave(h.player); HeroManager.remove(h.player); });
        ServerPlayConnectionEvents.JOIN.register((h, sender, s) -> MatchManager.onJoin(h.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldP, newP, alive) -> {
            HeroManager.onRespawn(newP);
            MatchManager.onRespawn(newP);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((e, src) -> { if (e instanceof ServerPlayerEntity || e instanceof com.minewatch.entity.BotEntity) MatchManager.onDeath(e, src); });
        ServerLifecycleCleanup.register();

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((e, src, amt) -> {
            // 같은 팀(플레이어/봇) 아군 피해 차단
            if (src.getAttacker() != null && src.getAttacker() != e && MatchManager.sameTeam(src.getAttacker(), e)) return false;
            // 리콜 등 무적 상태
            if (e instanceof ServerPlayerEntity v) {
                var hs = HeroManager.stateOf(v);
                return hs == null || !hs.invulnerable();
            }
            return true;
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> Commands.register(dispatcher));
    }

    private static void registerNetworking() {
        PayloadTypeRegistry.playC2S().register(InputPayload.ID, InputPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SelectHeroPayload.ID, SelectHeroPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PartyActionPayload.ID, PartyActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StatePayload.ID, StatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MatchPayload.ID, MatchPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(KillFeedPayload.ID, KillFeedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PoolsPayload.ID, PoolsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(com.minewatch.net.QueuePayload.ID, com.minewatch.net.QueuePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HitPayload.ID, HitPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DamageDirPayload.ID, DamageDirPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(InputPayload.ID,
                (payload, ctx) -> HeroManager.setInput(ctx.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(SelectHeroPayload.ID, (payload, ctx) -> {
            if (!MatchManager.canChangeHero()) {
                ctx.player().sendMessage(Text.literal("매치 진행 중에는 영웅을 바꿀 수 없습니다."), true);
                return;
            }
            HeroManager.select(ctx.player(), HeroRegistry.get(payload.heroId()));
        });
        ServerPlayNetworking.registerGlobalReceiver(PartyActionPayload.ID,
                (payload, ctx) -> PartyActions.handle(ctx.player(), payload));
    }
}
