package com.minewatch.hero;

import com.minewatch.net.InputPayload;
import com.minewatch.net.StatePayload;
import com.minewatch.server.OwDamage;
import com.minewatch.server.PulseBombs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/** 트레이서: 펄스 쌍권총 / 블링크 / 리콜 / 펄스 폭탄. */
public class Tracer extends Hero {
    public static final int MAX_AMMO = 40;
    public static final int RELOAD_TICKS = 20;          // 1.0초
    public static final double BULLET_DAMAGE = 6;       // OW 피해 / 발
    public static final double BULLETS_PER_TICK = 2;    // 초당 40발
    public static final double RANGE = 64;
    public static final double FALLOFF_START = 15, FALLOFF_END = 30;

    public static final int MAX_BLINK = 3;
    public static final int BLINK_RECHARGE_TICKS = 60;  // 3초
    public static final double BLINK_DISTANCE = 7;
    public static final double BLINK_STEP = 0.25;

    public static final int HISTORY_TICKS = 60;         // 3초 * 20
    public static final int RECALL_COOLDOWN = 240;      // 12초
    public static final int RECALL_STEPS_PER_TICK = 3;  // 60개 기록을 20틱에 되감기

    public static final double MELEE_DAMAGE = 30, MELEE_RANGE = 3.0;
    public static final int MELEE_COOLDOWN = 20;

    public Tracer() { super(1, "tracer"); }

    @Override public net.minecraft.item.Item weapon() { return com.minewatch.ModItems.PULSE_PISTOLS; }

    @Override public double ultCost() { return 1000; }
    @Override public HeroState createState() { return new TracerState(); }

    @Override
    public void onSelect(ServerPlayerEntity p, HeroState hs) {
        TracerState s = (TracerState) hs;
        p.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(20);
        p.setHealth(20);
        boolean has = false;
        for (int i = 0; i < p.getInventory().size(); i++)
            if (p.getInventory().getStack(i).isOf(com.minewatch.ModItems.PULSE_PISTOLS)) has = true;
        if (!has) p.getInventory().setStack(0, new ItemStack(com.minewatch.ModItems.PULSE_PISTOLS));
        p.getInventory().selectedSlot = 0;
        s.ammo = MAX_AMMO;
    }

    @Override
    public void tick(ServerPlayerEntity p, HeroState hs) {
        TracerState s = (TracerState) hs;
        ServerWorld world = p.getServerWorld();

        // 궁극기 자연 충전
        s.ultPoints = Math.min(ultCost(), s.ultPoints + passiveUltPerSecond() / 20.0);

        if (s.recallCooldown > 0) s.recallCooldown--;
        if (s.meleeCooldown > 0) s.meleeCooldown--;
        if (s.blinkCharges < MAX_BLINK && ++s.blinkTimer >= BLINK_RECHARGE_TICKS) { s.blinkCharges++; s.blinkTimer = 0; }
        if (s.blinkCharges >= MAX_BLINK) s.blinkTimer = 0;

        if (s.recalling) { tickRecall(p, s, world); return; }

        // 3초 기록 (20Hz): 위치, 시점, 체력, 탄약
        s.history.addLast(new TracerState.Snapshot(p.getPos(), p.getYaw(), p.getPitch(), p.getHealth(), s.ammo));
        while (s.history.size() > HISTORY_TICKS) s.history.removeFirst();

        boolean holding = p.getMainHandStack().isOf(com.minewatch.ModItems.PULSE_PISTOLS);

        // 장전
        if (s.reloadTicks > 0) { if (--s.reloadTicks == 0) s.ammo = MAX_AMMO; }
        else if (holding && (s.pressed(InputPayload.RELOAD) || s.ammo == 0) && s.ammo < MAX_AMMO) startReload(p, s);

        // 사격
        if (holding && s.reloadTicks == 0 && s.held(InputPayload.FIRE) && s.ammo > 0) {
            s.bulletAcc += BULLETS_PER_TICK;
            while (s.bulletAcc >= 1 && s.ammo > 0) { s.bulletAcc -= 1; s.ammo--; fire(p, s, world); }
            if (p.age % 2 == 0) com.minewatch.server.Sfx.at(p, "pulse_fire_crack", 0.5f, 1f);
        } else s.bulletAcc = 0;

        if (s.pressed(InputPayload.ABILITY1)) blink(p, s, world);
        if (s.pressed(InputPayload.ABILITY2)) startRecall(p, s, world);
        if (s.pressed(InputPayload.MELEE)) melee(p, s);
        if (s.pressed(InputPayload.ULT) && s.ultPoints >= ultCost()) {
            s.ultPoints = 0;
            PulseBombs.throwBomb(p);
        }
    }

    private void startReload(ServerPlayerEntity p, TracerState s) {
        s.reloadTicks = RELOAD_TICKS;
        com.minewatch.server.Sfx.at(p, "reload_open", 0.8f, 1f);
    }

    // ---- 펄스 쌍권총 (히트스캔) ----
    private void fire(ServerPlayerEntity p, TracerState s, ServerWorld world) {
        Vec3d eye = p.getEyePos();
        Vec3d dir = p.getRotationVec(1.0f);
        Vec3d end = eye.add(dir.multiply(RANGE));

        BlockHitResult blockHit = world.raycast(new RaycastContext(eye, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
        Vec3d limit = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getPos();

        Box sweep = p.getBoundingBox().stretch(limit.subtract(eye)).expand(1.0);
        EntityHitResult eh = ProjectileUtil.raycast(p, eye, limit, sweep,
                e -> e instanceof LivingEntity le && le.isAlive() && !e.isSpectator() && e != p, RANGE * RANGE);
        Vec3d impact = limit;
        if (eh != null) {
            Entity t = eh.getEntity();
            impact = eh.getPos();
            double dist = eye.distanceTo(impact);
            boolean head = impact.y >= t.getY() + t.getHeight() * 0.8;
            double dmg = BULLET_DAMAGE * falloff(dist) * (head ? 2.0 : 1.0);
            double dealt = OwDamage.deal(p, (LivingEntity) t, dmg, head, false);
            s.ultPoints = Math.min(ultCost(), s.ultPoints + dealt);
        }

        s.rightHand = !s.rightHand;
        // 총구 섬광 + 탄도 궤적
        Vec3d side = new Vec3d(-dir.z, 0, dir.x).normalize().multiply(s.rightHand ? 0.3 : -0.3);
        Vec3d muzzle = eye.add(dir.multiply(0.8)).add(side).add(0, -0.25, 0);
        com.minewatch.server.Fx.spawn(world, "muzzle", muzzle, dir, 1f, 3);
        com.minewatch.server.Fx.bullet(world, muzzle, impact, "cyan");
        if (eh == null && blockHit.getType() != HitResult.Type.MISS)
            com.minewatch.server.Fx.spawn(world, "impact", impact, dir, 1f, 5);
    }

    /** 15m 까지 100%, 30m 에서 50% 까지 선형 감소. */
    public static double falloff(double dist) {
        if (dist <= FALLOFF_START) return 1.0;
        if (dist >= FALLOFF_END) return 0.5;
        return 1.0 - 0.5 * (dist - FALLOFF_START) / (FALLOFF_END - FALLOFF_START);
    }

    // ---- 블링크 ----
    private void blink(ServerPlayerEntity p, TracerState s, ServerWorld world) {
        if (s.blinkCharges <= 0) return;
        InputPayload in = s.input;
        float yawRad = p.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        double fx = -MathHelper.sin(yawRad), fz = MathHelper.cos(yawRad);   // 앞
        double lx = MathHelper.cos(yawRad), lz = MathHelper.sin(yawRad);    // 왼쪽
        double dx = fx * in.forward() + lx * in.sideways();
        double dz = fz * in.forward() + lz * in.sideways();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-4) { dx = fx; dz = fz; len = 1; }   // 입력 없으면 정면(수평)
        dx /= len; dz /= len;

        Vec3d start = p.getPos();
        Box box = p.getBoundingBox();
        double moved = 0, ox = 0, oz = 0;
        while (moved + BLINK_STEP <= BLINK_DISTANCE + 1e-6) {
            Box next = box.offset(ox + dx * BLINK_STEP, 0, oz + dz * BLINK_STEP);
            if (!world.isSpaceEmpty(p, next)) break;    // 벽 직전에서 정지
            ox += dx * BLINK_STEP; oz += dz * BLINK_STEP; moved += BLINK_STEP;
        }
        s.blinkCharges--;
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, start.x, start.y + 1, start.z, 12, 0.3, 0.6, 0.3, 0.1);
        com.minewatch.server.Fx.spawn(world, "blink_flash", start.add(0, 1, 0), new Vec3d(0, 0, 1), 1f, 9);
        if (moved <= 0) return;
        com.minewatch.server.Fx.spawn(world, "blink_trail", start.add(0, 1, 0), new Vec3d(dx, 0, dz), (float) moved, 10);
        p.networkHandler.requestTeleport(start.x + ox, start.y, start.z + oz, p.getYaw(), p.getPitch());
        p.setVelocity(p.getVelocity().multiply(0.2, 1, 0.2));
        p.velocityModified = true;
        com.minewatch.server.Fx.spawn(world, "blink_flash", start.add(ox, 1, oz), new Vec3d(0, 0, 1), 1f, 9);
        com.minewatch.server.Sfx.at(p, "blink", 0.8f, 1f);
    }

    // ---- 리콜 ----
    private void startRecall(ServerPlayerEntity p, TracerState s, ServerWorld world) {
        if (s.recallCooldown > 0 || s.history.size() < 5) return;
        float best = p.getHealth();
        for (TracerState.Snapshot sn : s.history) best = Math.max(best, sn.health);
        s.recallHealth = best;
        s.recalling = true;
        s.recallOrigin = p.getPos();
        s.recallTicks = 0;
        s.recallCooldown = (int) Math.round(RECALL_COOLDOWN / s.cdRate[1]);
        s.reloadTicks = 0;
        com.minewatch.server.Sfx.at(p, "recall_start", 1f, 1f);
        com.minewatch.server.Fx.spawn(world, "recall_ring", s.recallOrigin, new Vec3d(0, 0, 1), 1f, 26);
    }

    private void tickRecall(ServerPlayerEntity p, TracerState s, ServerWorld world) {
        s.recallTicks++;
        Vec3d o = s.recallOrigin;
        // 출발 지점의 시간 소용돌이
        double ang = s.recallTicks * 0.9;
        for (int i = 0; i < 3; i++) {
            double a = ang + i * 2.094, r = 0.8 * (1 - s.recallTicks / 24.0);
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, o.x + Math.cos(a) * r, o.y + 1 + i * 0.3 - 0.3, o.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
        p.fallDistance = 0;
        TracerState.Snapshot last = null;
        for (int i = 0; i < RECALL_STEPS_PER_TICK && !s.history.isEmpty(); i++) last = s.history.pollLast();
        if (last != null) {
            p.networkHandler.requestTeleport(last.pos.x, last.pos.y, last.pos.z, last.yaw, last.pitch);
            p.setVelocity(Vec3d.ZERO);
            p.velocityModified = true;
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, last.pos.x, last.pos.y + 1, last.pos.z, 3, 0.3, 0.5, 0.3, 0.05);
        }
        if (s.history.isEmpty()) {
            s.recalling = false;
            p.setHealth(s.recallFullHeal ? p.getMaxHealth() : Math.max(p.getHealth(), s.recallHealth));
            s.ammo = MAX_AMMO;
            com.minewatch.server.Fx.spawn(world, "recall_ring", p.getPos(), new Vec3d(0, 0, 1), 1f, 26);
            world.spawnParticles(ParticleTypes.END_ROD, o.x, o.y + 1, o.z, 12, 0.1, 0.1, 0.1, 0.25);
            com.minewatch.server.Sfx.at(p, "recall_arrive", 1f, 1f);
        }
    }

    // ---- 근접 공격 ----
    private void melee(ServerPlayerEntity p, TracerState s) {
        if (s.meleeCooldown > 0) return;
        s.meleeCooldown = MELEE_COOLDOWN;
        Vec3d eye = p.getEyePos();
        Vec3d end = eye.add(p.getRotationVec(1f).multiply(MELEE_RANGE));
        EntityHitResult eh = ProjectileUtil.raycast(p, eye, end, p.getBoundingBox().stretch(end.subtract(eye)).expand(1),
                e -> e instanceof LivingEntity le && le.isAlive() && e != p, MELEE_RANGE * MELEE_RANGE);
        p.swingHand(net.minecraft.util.Hand.MAIN_HAND, true);
        if (eh != null) {
            double dealt = OwDamage.deal(p, (LivingEntity) eh.getEntity(), MELEE_DAMAGE, false, true);
            s.ultPoints = Math.min(ultCost(), s.ultPoints + dealt);
        }
    }

    @Override public void onDeath(ServerPlayerEntity p, HeroState hs) {
        TracerState s = (TracerState) hs;
        s.history.clear(); s.recalling = false; s.ammo = MAX_AMMO; s.reloadTicks = 0;
    }

    public boolean isRecalling(HeroState hs) { return ((TracerState) hs).recalling; }

    @Override
    public void reloadNow(ServerPlayerEntity p, HeroState hs) {
        TracerState s = (TracerState) hs;
        s.ammo = MAX_AMMO; s.reloadTicks = 0;
    }

    @Override
    public StatePayload toPayload(HeroState hs) {
        TracerState s = (TracerState) hs;
        float blinkCd = s.blinkCharges >= MAX_BLINK ? 0f : 1f - s.blinkTimer / (float) BLINK_RECHARGE_TICKS;
        return new StatePayload(numericId, s.ammo, MAX_AMMO, s.reloadTicks > 0, (float) (s.ultPoints / ultCost()), false, 0,
                java.util.List.of(
                        new StatePayload.Slot("blink", blinkCd, s.blinkCharges, MAX_BLINK, false),
                        new StatePayload.Slot("recall", s.recallCooldown / (float) RECALL_COOLDOWN, 0, 0, s.recalling)));
    }
}
