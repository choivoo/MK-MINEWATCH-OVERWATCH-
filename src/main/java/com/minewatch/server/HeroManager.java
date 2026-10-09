package com.minewatch.server;

import com.minewatch.hero.Hero;
import com.minewatch.hero.HeroState;
import com.minewatch.net.StatePayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class HeroManager {
    private static final class Entry { Hero hero; HeroState state; }
    private static final Map<UUID, Entry> ENTRIES = new HashMap<>();

    public static Hero heroOf(ServerPlayerEntity p) {
        Entry e = ENTRIES.get(p.getUuid());
        return e == null ? null : e.hero;
    }
    public static HeroState stateOf(ServerPlayerEntity p) {
        Entry e = ENTRIES.get(p.getUuid());
        return e == null ? null : e.state;
    }

    public static void select(ServerPlayerEntity p, Hero hero) {
        Entry old = ENTRIES.remove(p.getUuid());
        if (old != null) old.hero.onDeselect(p, old.state);
        if (hero == null) { ServerPlayNetworking.send(p, StatePayload.NONE); return; }
        Entry e = new Entry();
        e.hero = hero; e.state = hero.createState();
        ENTRIES.put(p.getUuid(), e);
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
            if (!p.isAlive() || p.isSpectator()) { e.hero.onDeath(p, e.state); continue; }
            e.hero.tick(p, e.state);
            e.state.prevInput = e.state.input;
            ServerPlayNetworking.send(p, e.hero.toPayload(e.state));
        }
    }
    private HeroManager() {}
}
