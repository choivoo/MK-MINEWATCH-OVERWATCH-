package com.minewatch.hero;

import com.minewatch.ModItems;
import com.minewatch.net.InputPayload;
import com.minewatch.net.StatePayload;
import com.minewatch.server.OwDamage;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/** 라인하르트: 로켓 망치 / 방벽(우클릭 유지) / 돌진(Shift) / 화염 강타(E) / 대지 분쇄(Q). */
public class Reinhardt extends Hero {
    public static final double SWING_DAMAGE = 75, SWING_RANGE = 4.5, SWING_HALF_ANGLE = 50;
    public static final int SWING_COOLDOWN = 26;
    public static final double BARRIER_HP = 1200, BARRIER_REGEN_PER_TICK = 11.25;
    public static final int CHARGE_COOLDOWN = 200, CHARGE_TICKS = 20;
    public static final double CHARGE_STEP = 0.9, CHARGE_PIN_DAMAGE = 50, CHARGE_WALL_DAMAGE = 300;
    public static final int FIRESTRIKE_COOLDOWN = 120;
    public static final double FIRESTRIKE_DAMAGE = 100, FIRESTRIKE_RANGE = 18;
    public static final double QUAKE_DAMAGE = 250, QUAKE_RANGE = 22, QUAKE_HALF_ANGLE = 28;
    public static final int QUAKE_STUN = 50;

    static final class State extends HeroState {
        LivingEntity pinned;     // 돌진으로 붙잡은 대상
        Vec3d chargeDir = Vec3d.ZERO;
    }

    public Reinhardt() { super(4, "reinhardt"); }

    @Override public Role role() { return Role.TANK; }
    @Override public double maxHealthOw() { return 325; }
    @Override public double maxArmor() { return 275; }
    @Override public double ultCost() { return 2000; }
    @Override public Item weapon() { return ModItems.ROCKET_HAMMER; }
    @Override public HeroState createState() { return new State(); }

    @Override
    public void onSelect(ServerPlayerEntity p, HeroState hs) {
        hs.barrierMax = BARRIER_HP; hs.barrierHp = BARRIER_HP;
    }

    @Override
    public void tick(ServerPlayerEntity p, HeroState hs) {
        State s = (State) hs;
        if (!prelude(s)) { s.barrierUp = false; return; }
        ServerWorld w = p.getServerWorld();
        boolean holding = p.getMainHandStack().getItem() == weapon();

        if (s.timer > 0) { tickCharge(p, s, w); return; }            // 돌진 중에는 다른 행동 불가

        // 방벽: 우클릭을 누르는 동안 올라간다. 파괴되면 재사용 대기.
        boolean wantBarrier = holding && s.held(InputPayload.ALT_FIRE) && s.barrierBroken == 0 && s.barrierHp > 0;
        s.barrierUp = wantBarrier;
        if (wantBarrier) p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 3, 1, false, false, false));
        else if (s.barrierHp < s.barrierMax && s.barrierBroken == 0) s.barrierHp = Math.min(s.barrierMax, s.barrierHp + BARRIER_REGEN_PER_TICK);

        if (holding && !s.barrierUp && s.held(InputPayload.FIRE) && s.cd[2] == 0) swing(p, s, w);
        if (s.pressed(InputPayload.ABILITY1) && s.cd[0] == 0 && !s.barrierUp) startCharge(p, s, w);
        if (s.pressed(InputPayload.ABILITY2) && s.cd[1] == 0) { fireStrike(p, s, w); s.cd[1] = FIRESTRIKE_COOLDOWN; }
        if (s.pressed(InputPayload.ULT) && s.ultPoints >= ultCost()) { s.ultPoints = 0; earthshatter(p, s, w); }
    }

    private void swing(ServerPlayerEntity p, State s, ServerWorld w) {
        s.cd[2] = SWING_COOLDOWN;
        w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, 0.7f);
        for (LivingEntity e : HeroKit.inCone(p, SWING_RANGE, SWING_HALF_ANGLE, x -> HeroKit.isEnemy(p, x)))
            s.addUlt(this, OwDamage.deal(p, e, SWING_DAMAGE, false, true));
    }

    private void startCharge(ServerPlayerEntity p, State s, ServerWorld w) {
        Vec3d look = p.getRotationVec(1f);
        s.chargeDir = new Vec3d(look.x, 0, look.z).normalize();
        s.timer = CHARGE_TICKS; s.pinned = null; s.cd[0] = CHARGE_COOLDOWN;
        w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.PLAYERS, 0.8f, 1.2f);
    }

    private void tickCharge(ServerPlayerEntity p, State s, ServerWorld w) {
        s.timer--;
        Vec3d before = p.getPos();
        boolean moved = HeroKit.moveToward(p, before.add(s.chargeDir.multiply(CHARGE_STEP)));
        boolean blocked = !moved || p.getPos().distanceTo(before) < CHARGE_STEP * 0.5;
        w.spawnParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(), 3, 0.3, 0.1, 0.3, 0.02);

        if (s.pinned == null) {
            for (LivingEntity e : HeroKit.inCone(p, 2.2, 70, x -> HeroKit.isEnemy(p, x))) { s.pinned = e; break; }
        }
        if (s.pinned != null && s.pinned.isAlive()) {
            Vec3d carry = p.getPos().add(s.chargeDir.multiply(1.3));
            HeroKit.moveToward(s.pinned, carry);
            if (blocked) {                                  // 벽에 부딪히면 큰 피해
                s.addUlt(this, OwDamage.deal(p, s.pinned, CHARGE_WALL_DAMAGE, false, false));
                s.pinned = null; s.timer = 0;
                w.playSound(null, p.getBlockPos(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 1.0f, 0.6f);
                return;
            }
        } else if (blocked) { s.timer = 0; }
        if (s.timer <= 0) {
            if (s.pinned != null && s.pinned.isAlive()) s.addUlt(this, OwDamage.deal(p, s.pinned, CHARGE_PIN_DAMAGE, false, true));
            s.pinned = null;
        }
    }

    private void fireStrike(ServerPlayerEntity p, State s, ServerWorld w) {
        Vec3d from = p.getEyePos().add(p.getRotationVec(1f).multiply(0.8));
        HeroKit.beam(w, from, from.add(p.getRotationVec(1f).multiply(FIRESTRIKE_RANGE)), 1f, 0.5f, 0.1f, 1.2f);
        w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.8f);
        for (LivingEntity e : HeroKit.traceAll(p, FIRESTRIKE_RANGE, 0.6, x -> HeroKit.isEnemy(p, x)))
            s.addUlt(this, OwDamage.deal(p, e, FIRESTRIKE_DAMAGE, false, false));
    }

    private void earthshatter(ServerPlayerEntity p, State s, ServerWorld w) {
        w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 1.2f, 0.5f);
        Vec3d f = p.getRotationVec(1f);
        for (int d = 2; d <= (int) QUAKE_RANGE; d += 3)
            w.spawnParticles(ParticleTypes.EXPLOSION, p.getX() + f.x * d, p.getY() + 0.3, p.getZ() + f.z * d, 1, 1.0, 0, 1.0, 0);
        for (LivingEntity e : HeroKit.inCone(p, QUAKE_RANGE, QUAKE_HALF_ANGLE, x -> HeroKit.isEnemy(p, x))) {
            if (!p.canSee(e)) continue;
            HeroKit.stun(e, QUAKE_STUN);
            s.addUlt(this, OwDamage.deal(p, e, QUAKE_DAMAGE, false, false));
        }
    }

    @Override
    public void onDeath(ServerPlayerEntity p, HeroState hs) {
        State s = (State) hs;
        s.timer = 0; s.pinned = null; s.barrierUp = false; s.barrierHp = BARRIER_HP; s.barrierBroken = 0;
    }

    @Override
    public StatePayload toPayload(HeroState hs) {
        State s = (State) hs;
        int barrierPct = (int) Math.round(s.barrierHp * 100.0 / BARRIER_HP);
        return payload(s, 0, 0, false, false, barrierPct,
                new StatePayload.Slot("charge", frac(s.cd[0], CHARGE_COOLDOWN), 0, 0, s.timer > 0),
                new StatePayload.Slot("firestrike", frac(s.cd[1], FIRESTRIKE_COOLDOWN), 0, 0, false));
    }
}
