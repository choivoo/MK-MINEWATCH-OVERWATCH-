package com.minewatch.server;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/** 서버가 시뮬레이션하는 힐팩. 대형 250 / 소형 75, 습득 후 10초 뒤 재생성. */
public final class HealthPacks {
    public static final int LARGE_HEAL = 250, SMALL_HEAL = 75, RESPAWN_TICKS = 200;
    private static final double RADIUS = 1.5;

    private static final class Pack {
        ServerWorld world; Vec3d pos; boolean large; int cooldown;
    }
    private static final List<Pack> PACKS = new ArrayList<>();

    public static void place(ServerWorld world, Vec3d pos, boolean large) {
        if (world == null) return;
        Pack p = new Pack();
        p.world = world; p.pos = pos; p.large = large;
        PACKS.add(p);
    }

    public static int removeAll() { int n = PACKS.size(); PACKS.clear(); return n; }

    public static void tick(MinecraftServer server) {
        PACKS.removeIf(k -> k.world == null);
        for (Pack k : PACKS) {
            if (k.cooldown > 0) { k.cooldown--; continue; }
            if (k.world.getTime() % 5 == 0)
                k.world.spawnParticles(k.large ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.COMPOSTER,
                        k.pos.x, k.pos.y + 0.6, k.pos.z, k.large ? 4 : 2, 0.25, 0.4, 0.25, 0.0);
            for (ServerPlayerEntity pl : k.world.getPlayers()) {
                if (!pl.isAlive() || pl.getHealth() >= pl.getMaxHealth()) continue;
                if (Math.abs(pl.getY() - k.pos.y) > 2.0) continue;
                double dx = pl.getX() - k.pos.x, dz = pl.getZ() - k.pos.z;
                if (dx * dx + dz * dz > RADIUS * RADIUS) continue;
                OwHeal.heal(pl, k.large ? LARGE_HEAL : SMALL_HEAL);
                var hs = HeroManager.stateOf(pl);
                if (hs instanceof com.minewatch.hero.TracerState ts && ts.healpackBlink && ts.blinkCharges < com.minewatch.hero.Tracer.MAX_BLINK) ts.blinkCharges++;
                Sfx.play(pl, "health_pack_pickup", 0.8f, 1f);
                k.cooldown = RESPAWN_TICKS;
                break;
            }
        }
    }

    public static void clear() { PACKS.clear(); }
    private HealthPacks() {}
}
