package com.minewatch.server;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/** 펄스 폭탄: 포물선 투척, 블록/적에 부착, 폭발. 서버에서 직접 시뮬레이션한다. */
public final class PulseBombs {
    private static final double GRAVITY = 0.04;      // blocks/tick^2 (≈16 b/s²)
    private static final double SPEED = 0.62;        // blocks/tick (≈12.4 b/s)
    private static final double LIFT = 0.12;         // 위쪽 보정(던지는 각도)
    private static final int FUSE_TICKS = 24;        // 부착 후 약 1.2초
    private static final double RADIUS = 4.0, FULL_RADIUS = 1.0;
    private static final double DAMAGE = 350;

    private static final class Bomb {
        ServerWorld world; UUID owner; Vec3d pos, vel;
        com.minewatch.entity.FxEntity fx; UUID stuckTo; Vec3d stuckOffset; boolean stuck; int fuse = FUSE_TICKS; int age;
    }
    private static final List<Bomb> BOMBS = new ArrayList<>();

    public static void throwBomb(ServerPlayerEntity p) {
        Bomb b = new Bomb();
        b.world = p.getServerWorld();
        b.owner = p.getUuid();
        Vec3d look = p.getRotationVec(1f);
        b.pos = p.getEyePos().add(look.multiply(0.5));
        b.vel = look.multiply(SPEED).add(0, LIFT, 0);
        b.fx = Fx.spawn(b.world, "bomb", b.pos, new Vec3d(0, 0, 1), 1f, 400);
        BOMBS.add(b);
        Sfx.at(p, "bomb_throw", 1f, 1f);
    }

    public static void clear() { for (Bomb b : BOMBS) if (b.fx != null) b.fx.discard(); BOMBS.clear(); }

    public static void tick() {
        BOMBS.removeIf(PulseBombs::tickBomb);
    }

    /** true 를 반환하면 제거. */
    private static boolean tickBomb(Bomb b) {
        ServerPlayerEntity owner = b.world.getServer().getPlayerManager().getPlayer(b.owner);
        b.age++;
        if (b.fx != null && !b.fx.isRemoved()) b.fx.refreshPositionAndAngles(b.pos.x, b.pos.y, b.pos.z, 0, 0);
        if (!b.stuck) {
            if (b.age > 200) { if (b.fx != null) b.fx.discard(); return true; }
            b.vel = b.vel.subtract(0, GRAVITY, 0);
            Vec3d next = b.pos.add(b.vel);
            // 적 충돌
            Box seg = new Box(b.pos, next).expand(0.4);
            for (Entity e : b.world.getOtherEntities(owner, seg, en -> en instanceof LivingEntity le && le.isAlive() && !en.isSpectator())) {
                if (e.getBoundingBox().expand(0.2).raycast(b.pos, next).isPresent()) {
                    stick(b, e.getBoundingBox().expand(0.2).raycast(b.pos, next).get(), e);
                    return false;
                }
            }
            BlockHitResult hit = b.world.raycast(new RaycastContext(b.pos, next, RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, owner != null ? owner : null));
            if (hit.getType() != HitResult.Type.MISS) { stick(b, hit.getPos(), null); return false; }
            b.pos = next;
            return false;
        }
        // 부착 중: 적이 움직이면 같이 움직임
        if (b.stuckTo != null) {
            Entity e = b.world.getEntity(b.stuckTo);
            if (e == null || !e.isAlive()) b.stuckTo = null; else b.pos = e.getPos().add(b.stuckOffset);
        }
        b.world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, b.pos.x, b.pos.y, b.pos.z, 2, 0.1, 0.1, 0.1, 0.02);
        if (b.fuse % 6 == 0) b.world.playSound(null, b.pos.x, b.pos.y, b.pos.z, SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundCategory.PLAYERS, 1f, 2f);
        if (--b.fuse <= 0) { explode(b, owner); return true; }
        return false;
    }

    private static void stick(Bomb b, Vec3d at, Entity e) {
        b.stuck = true; b.pos = at; b.vel = Vec3d.ZERO;
        if (e != null) { b.stuckTo = e.getUuid(); b.stuckOffset = at.subtract(e.getPos()); }
        Sfx.at(b.world, at.x, at.y, at.z, "bomb_stick", 1f, 1f);
    }

    private static void explode(Bomb b, ServerPlayerEntity owner) {
        Vec3d c = b.pos;
        if (b.fx != null) b.fx.discard();
        Fx.spawn(b.world, "bomb_blast", c, new Vec3d(0, 0, 1), 1f, 15);
        b.world.spawnParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 25, 1.5, 1.5, 1.5, 0.3);
        Sfx.at(b.world, c.x, c.y, c.z, "bomb_explode", 1.5f, 1f);
        if (owner == null) return;
        Box area = new Box(c, c).expand(RADIUS);
        for (Entity e : b.world.getOtherEntities(owner, area, en -> en instanceof LivingEntity le && le.isAlive())) {
            double d = e.getBoundingBox().getCenter().distanceTo(c);
            if (d > RADIUS) continue;
            double scale = d <= FULL_RADIUS ? 1.0 : 1.0 - 0.7 * (d - FULL_RADIUS) / (RADIUS - FULL_RADIUS);
            OwDamage.deal(owner, (LivingEntity) e, DAMAGE * scale, false, true);
        }
    }
    private PulseBombs() {}
}
