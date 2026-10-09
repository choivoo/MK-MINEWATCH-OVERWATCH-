package com.minewatch.hero;

import com.minewatch.entity.BotEntity;
import com.minewatch.server.HeroManager;
import com.minewatch.server.MatchManager;
import com.minewatch.server.OwDamage;
import com.minewatch.server.OwHeal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import org.joml.Vector3f;

/** 영웅 능력 구현에 공통으로 쓰는 서버측 도구 모음. */
public final class HeroKit {
    /** 조준선 추적 결과. entity 가 null 이면 블록(또는 최대 사거리) 지점. */
    public record Hit(LivingEntity entity, Vec3d point, boolean head, double dist) {}

    // ---- 진영 판별 ----

    /** 매치 팀이 다르면 적. 매치 밖에서는 플레이어가 아닌 대상(봇/몹)만 적으로 본다. */
    public static boolean isEnemy(LivingEntity self, LivingEntity other) {
        if (other == self || !other.isAlive()) return false;
        if (other instanceof ServerPlayerEntity sp && sp.isSpectator()) return false;
        int a = MatchManager.teamOfEntity(self), b = MatchManager.teamOfEntity(other);
        if (a >= 0 && b >= 0) return a != b;
        return !(other instanceof PlayerEntity);
    }

    /** 같은 팀이면 아군. 매치 밖에서는 다른 플레이어가 아군. */
    public static boolean isAlly(LivingEntity self, LivingEntity other) {
        if (other == self || !other.isAlive()) return false;
        int a = MatchManager.teamOfEntity(self), b = MatchManager.teamOfEntity(other);
        if (a >= 0 && b >= 0) return a == b;
        return a < 0 && b < 0 && other instanceof ServerPlayerEntity;
    }

    // ---- 조준선 ----

    private static Vec3d spread(Vec3d dir, double spreadDeg, Random r) {
        if (spreadDeg <= 0) return dir;
        Vec3d helper = Math.abs(dir.y) > 0.99 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0);
        Vec3d right = dir.crossProduct(helper).normalize();
        Vec3d up = right.crossProduct(dir).normalize();
        double radius = Math.tan(Math.toRadians(spreadDeg)) * Math.sqrt(r.nextDouble());
        double ang = r.nextDouble() * Math.PI * 2;
        return dir.add(right.multiply(Math.cos(ang) * radius)).add(up.multiply(Math.sin(ang) * radius)).normalize();
    }

    /** 시선(+퍼짐) 방향으로 한 줄을 쏴 처음 닿는 대상 또는 블록 지점을 찾는다. */
    public static Hit trace(ServerPlayerEntity p, double range, double spreadDeg, Predicate<LivingEntity> filter) {
        ServerWorld w = p.getServerWorld();
        Vec3d eye = p.getEyePos();
        Vec3d dir = spread(p.getRotationVec(1f), spreadDeg, p.getRandom());
        Vec3d end = eye.add(dir.multiply(range));
        BlockHitResult bh = w.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
        Vec3d limit = bh.getType() == HitResult.Type.MISS ? end : bh.getPos();
        Box box = p.getBoundingBox().stretch(dir.multiply(range)).expand(1.0);
        EntityHitResult eh = ProjectileUtil.raycast(p, eye, limit, box,
                e -> e instanceof LivingEntity le && le.isAlive() && !e.isSpectator() && e != p && filter.test(le), range * range);
        if (eh != null) {
            LivingEntity le = (LivingEntity) eh.getEntity();
            Vec3d pt = eh.getPos();
            return new Hit(le, pt, pt.y >= le.getEyeY() - 0.25, eye.distanceTo(pt));
        }
        return new Hit(null, limit, false, eye.distanceTo(limit));
    }

    /** 시선 방향 한 줄 위의 모든 대상(관통). 블록에 막히면 거기까지. 가까운 순. */
    public static List<LivingEntity> traceAll(ServerPlayerEntity p, double range, double radius, Predicate<LivingEntity> filter) {
        ServerWorld w = p.getServerWorld();
        Vec3d eye = p.getEyePos(), dir = p.getRotationVec(1f);
        Vec3d end = eye.add(dir.multiply(range));
        BlockHitResult bh = w.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
        Vec3d limit = bh.getType() == HitResult.Type.MISS ? end : bh.getPos();
        Box box = new Box(eye, limit).expand(radius + 1);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box, le -> le != p && le.isAlive() && filter.test(le)))
            if (e.getBoundingBox().expand(radius).raycast(eye, limit).isPresent()) out.add(e);
        out.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(eye)));
        return out;
    }

    /** 시선 앞쪽 부채꼴 안의 대상(근접 공격/충격파 등). */
    public static List<LivingEntity> inCone(ServerPlayerEntity p, double range, double halfAngleDeg, Predicate<LivingEntity> filter) {
        ServerWorld w = p.getServerWorld();
        Vec3d eye = p.getEyePos(), look = p.getRotationVec(1f);
        double cos = Math.cos(Math.toRadians(halfAngleDeg));
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(range), le -> le != p && le.isAlive() && filter.test(le))) {
            Vec3d to = e.getBoundingBox().getCenter().subtract(eye);
            double d = to.length();
            if (d > range + e.getWidth() / 2 || d < 1e-6) continue;
            if (to.normalize().dotProduct(look) >= cos) out.add(e);
        }
        out.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(p)));
        return out;
    }

    public static List<LivingEntity> inRadius(ServerWorld w, Vec3d center, double r, Predicate<LivingEntity> filter) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(center, center).expand(r), le -> le.isAlive() && filter.test(le)))
            if (e.getBoundingBox().getCenter().distanceTo(center) <= r + e.getWidth() / 2) out.add(e);
        return out;
    }

    // ---- 치유 / 군중 제어 / 이동 ----

    /** 대상을 오버워치 단위로 치유하고 실제 치유량을 반환한다. */
    public static double heal(LivingEntity target, double ow) {
        if (ow <= 0 || !target.isAlive()) return 0;
        if (target instanceof ServerPlayerEntity tp) return OwHeal.heal(tp, ow);
        float before = target.getHealth();
        target.heal((float) (ow / Hero.HP_SCALE));
        return (target.getHealth() - before) * Hero.HP_SCALE;
    }

    /** 기절(이동/점프/입력 불가). */
    public static void stun(LivingEntity e, int ticks) {
        e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, ticks, 7, false, false, false));
        e.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, ticks, 128, false, false, false));
        if (e instanceof BotEntity b) b.stun(ticks);
        else if (e instanceof ServerPlayerEntity sp) {
            HeroState hs = HeroManager.stateOf(sp);
            if (hs != null) hs.stunTicks = Math.max(hs.stunTicks, ticks);
        }
    }

    /** 목적지까지 충돌 없는 가장 먼 지점으로 이동(순간이동). 실제로 이동했으면 true. */
    public static boolean moveToward(LivingEntity e, Vec3d dest) {
        ServerWorld w = (ServerWorld) e.getWorld();
        Vec3d from = e.getPos(), delta = dest.subtract(from);
        for (double f = 1.0; f >= 0.1; f -= 0.1) {
            Vec3d step = delta.multiply(f);
            if (w.isSpaceEmpty(e, e.getBoundingBox().offset(step))) {
                Vec3d to = from.add(step);
                if (e instanceof ServerPlayerEntity sp) sp.requestTeleport(to.x, to.y, to.z);
                else e.refreshPositionAfterTeleport(to.x, to.y, to.z);
                return true;
            }
        }
        return false;
    }

    // ---- 연출 ----

    /** 총구에서 목표까지 날아가는 빛줄기(블록벤치 키프레임 연출). 색은 가장 가까운 총알 색으로 바꾼다. */
    public static void beam(ServerWorld w, Vec3d from, Vec3d to, float r, float g, float b, float size) {
        com.minewatch.server.Fx.bullet(w, from, to, com.minewatch.server.Fx.colorName(r, g, b));
    }

    /** 히트스캔 한 발: 대상에게 피해를 주고 궁극기를 충전한다. 맞춘 대상을 반환(없으면 null). */
    public static LivingEntity shoot(ServerPlayerEntity p, HeroState s, Hero hero, GunSpec g, double spreadDeg, double damageMult) {
        Hit h = trace(p, g.range(), spreadDeg, e -> isEnemy(p, e));
        if (h.entity() != null) {
            double dealt = OwDamage.deal(p, h.entity(), g.damageAt(h.dist(), h.head()) * damageMult, h.head(), false);
            s.addUlt(hero, dealt);
            return h.entity();
        }
        return null;
    }

    /** 영웅이 쓰는 모든 무기 아이템을 인벤토리에서 치운다. */
    public static void clearWeapons(ServerPlayerEntity p) {
        for (int i = 0; i < p.getInventory().size(); i++)
            if (p.getInventory().getStack(i).getItem() instanceof com.minewatch.HeroWeaponItem) p.getInventory().setStack(i, net.minecraft.item.ItemStack.EMPTY);
    }
    private HeroKit() {}
}
