package com.minewatch.server;

import com.minewatch.hero.HeroKit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/** 위도우메이커의 독 지뢰와 지속 피해. */
public final class VenomMines {
    public static final int MAX_PER_OWNER = 3, ARM_TICKS = 20, DOT_TICKS = 60, DOT_INTERVAL = 10;
    public static final double TRIGGER_RADIUS = 1.6, DOT_DAMAGE = 12, INITIAL_DAMAGE = 30;

    private static final class Mine { ServerWorld w; Vec3d pos; UUID owner; int age; }
    private static final class Dot { LivingEntity target; UUID owner; int ticks; }

    private static final List<Mine> MINES = new ArrayList<>();
    private static final List<Dot> DOTS = new ArrayList<>();

    public static void place(ServerWorld w, Vec3d pos, ServerPlayerEntity owner) {
        List<Mine> mine = new ArrayList<>();
        for (Mine m : MINES) if (m.owner.equals(owner.getUuid())) mine.add(m);
        if (mine.size() >= MAX_PER_OWNER) MINES.remove(mine.get(0));          // 가장 오래된 지뢰 제거
        Mine m = new Mine();
        m.w = w; m.pos = pos; m.owner = owner.getUuid();
        MINES.add(m);
    }

    public static int count() { return MINES.size(); }
    public static int dotCount() { return DOTS.size(); }

    public static void tick(MinecraftServer server) {
        if (MINES.isEmpty() && DOTS.isEmpty()) return;
        for (int i = MINES.size() - 1; i >= 0; i--) {
            Mine m = MINES.get(i);
            ServerPlayerEntity owner = server.getPlayerManager().getPlayer(m.owner);
            if (owner == null) { MINES.remove(i); continue; }
            m.age++;
            if (m.age % 10 == 0) m.w.spawnParticles(ParticleTypes.WITCH, m.pos.x, m.pos.y + 0.1, m.pos.z, 2, 0.15, 0.05, 0.15, 0);
            if (m.age < ARM_TICKS) continue;
            List<LivingEntity> hit = HeroKit.inRadius(m.w, m.pos, TRIGGER_RADIUS, e -> HeroKit.isEnemy(owner, e));
            if (hit.isEmpty()) continue;
            MINES.remove(i);
            m.w.playSound(null, m.pos.x, m.pos.y, m.pos.z, SoundEvents.ENTITY_SPLASH_POTION_BREAK, SoundCategory.PLAYERS, 0.8f, 0.7f);
            m.w.spawnParticles(ParticleTypes.WITCH, m.pos.x, m.pos.y + 0.3, m.pos.z, 20, 0.5, 0.3, 0.5, 0.05);
            for (LivingEntity e : hit) {
                OwDamage.deal(owner, e, INITIAL_DAMAGE, false, false);
                Dot d = new Dot();
                d.target = e; d.owner = m.owner; d.ticks = DOT_TICKS;
                DOTS.add(d);
            }
        }
        for (int i = DOTS.size() - 1; i >= 0; i--) {
            Dot d = DOTS.get(i);
            ServerPlayerEntity owner = server.getPlayerManager().getPlayer(d.owner);
            if (owner == null || !d.target.isAlive() || --d.ticks <= 0) { DOTS.remove(i); continue; }
            if (d.ticks % DOT_INTERVAL == 0) {
                OwDamage.deal(owner, d.target, DOT_DAMAGE, false, false);
                ((ServerWorld) d.target.getWorld()).spawnParticles(ParticleTypes.WITCH, d.target.getX(), d.target.getBodyY(0.5), d.target.getZ(), 4, 0.3, 0.4, 0.3, 0);
            }
        }
    }

    public static void clear() { MINES.clear(); DOTS.clear(); }
    private VenomMines() {}
}
