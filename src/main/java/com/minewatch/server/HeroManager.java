package com.minewatch.server;

import com.minewatch.hero.Hero;
import com.minewatch.hero.HeroState;
import com.minewatch.net.PoolsPayload;
import com.minewatch.net.StatePayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public final class HeroManager {
    private static final class Entry { Hero hero; HeroState state; PoolsPayload lastPools; }
    private static final Map<UUID, Entry> ENTRIES = new HashMap<>();

    public static Hero heroOf(ServerPlayerEntity p) {
        Entry e = ENTRIES.get(p.getUuid());
        return e == null ? null : e.hero;
    }
    public static HeroState stateOf(ServerPlayerEntity p) {
        Entry e = ENTRIES.get(p.getUuid());
        return e == null ? null : e.state;
    }

    private static void setMaxHealth(ServerPlayerEntity p, double mc) {
        EntityAttributeInstance a = p.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (a != null) a.setBaseValue(mc);
        p.setHealth(p.getMaxHealth());
    }

    public static void select(ServerPlayerEntity p, Hero hero) {
        Entry old = ENTRIES.remove(p.getUuid());
        if (old != null) old.hero.onDeselect(p, old.state);
        if (hero == null) {
            setMaxHealth(p, 20);
            ServerPlayNetworking.send(p, StatePayload.NONE);
            ServerPlayNetworking.send(p, PoolsPayload.NONE);
            return;
        }
        Entry e = new Entry();
        e.hero = hero; e.state = hero.createState();
        e.state.pools.init(hero.maxArmor(), hero.maxShield());
        ENTRIES.put(p.getUuid(), e);
        setMaxHealth(p, hero.maxHealthOw() / Hero.HP_SCALE);
        hero.onSelect(p, e.state);
    }

    public static void remove(ServerPlayerEntity p) { ENTRIES.remove(p.getUuid()); }
    public static void clear() { ENTRIES.clear(); }

    public static void setInput(ServerPlayerEntity p, com.minewatch.net.InputPayload in) {
        Entry e = ENTRIES.get(p.getUuid());
        if (e != null) e.state.input = in;
    }

    public static void tickAll(Iterable<ServerPlayerEntity> players) {
        for (ServerPlayerEntity p : players) {
            Entry e = ENTRIES.get(p.getUuid());
            if (e == null) continue;
            if (!p.isAlive() || p.isSpectator()) {
                e.hero.onDeath(p, e.state);
                e.state.pools.refill();      // 부활 시 방어구/보호막 가득
                e.state.ultReady = false;
                continue;
            }
            e.state.pools.tick();
            e.hero.tick(p, e.state);
            e.state.prevInput = e.state.input;
            notifyUlt(p, e);
            ServerPlayNetworking.send(p, e.hero.toPayload(e.state));
            var pl = e.state.pools;
            PoolsPayload pp = new PoolsPayload((int) Math.ceil(pl.armor), (int) pl.maxArmor, (int) Math.ceil(pl.shield), (int) pl.maxShield);
            if (!pp.equals(e.lastPools)) { e.lastPools = pp; ServerPlayNetworking.send(p, pp); }
        }
    }

    /** 궁극기가 가득 차는 순간 한 번 알림. */
    private static void notifyUlt(ServerPlayerEntity p, Entry e) {
        boolean ready = e.state.ultPoints >= e.hero.ultCost();
        if (ready && !e.state.ultReady) {
            p.playSoundToPlayer(SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.8f, 1.4f);
            p.sendMessage(Text.literal("궁극기 준비 완료!"), true);
        }
        e.state.ultReady = ready;
    }
    private HeroManager() {}
}
