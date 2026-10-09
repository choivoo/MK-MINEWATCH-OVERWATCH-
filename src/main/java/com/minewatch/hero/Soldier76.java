package com.minewatch.hero;

import com.minewatch.ModItems;
import com.minewatch.net.InputPayload;
import com.minewatch.net.StatePayload;
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

/** 솔저76: 펄스 소총 / 스프린트(Shift) / 헬릭스 로켓(E) / 전술 조준경(Q). */
public class Soldier76 extends Hero {
    public static final GunSpec GUN = new GunSpec(30, 30, 20, 2.0, 30, 55, 0.5, 70, 9, 1);
    public static final int HELIX_COOLDOWN = 160;       // 8초
    public static final double HELIX_DAMAGE = 120, HELIX_RADIUS = 2.5;
    public static final int VISOR_TICKS = 120;          // 6초
    public static final double VISOR_RANGE = 45, VISOR_CONE = 40;

    static final class State extends HeroState { int firing; }

    public Soldier76() { super(2, "soldier76"); }

    @Override public Role role() { return Role.DAMAGE; }
    @Override public double maxHealthOw() { return 200; }
    @Override public double ultCost() { return 1600; }
    @Override public Item weapon() { return ModItems.PULSE_RIFLE; }
    @Override public HeroState createState() { return new State(); }

    @Override public void onSelect(ServerPlayerEntity p, HeroState hs) { hs.gun.fill(GUN); }

    @Override
    public void tick(ServerPlayerEntity p, HeroState hs) {
        State s = (State) hs;
        if (!prelude(s)) return;
        ServerWorld w = p.getServerWorld();
        boolean holding = p.getMainHandStack().getItem() == weapon();
        boolean visor = s.timer > 0;
        if (visor) s.timer--;

        // 스프린트: Shift 로 켜고, 사격/장전/멈춤 시 해제
        if (s.pressed(InputPayload.ABILITY1)) s.flag = !s.flag;
        boolean firing = holding && s.held(InputPayload.FIRE);
        if (firing || s.gun.reload > 0 || (s.input.forward() <= 0.1f)) s.flag = false;
        if (s.flag) p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 3, 1, false, false, false));

        s.gun.tickReload(GUN);
        if (holding && (s.pressed(InputPayload.RELOAD) || s.gun.ammo == 0)) s.gun.startReload(GUN);

        boolean canFire = firing && s.gun.reload == 0;
        int shots = s.gun.shots(GUN, canFire);
        s.firing = canFire ? s.firing + 1 : 0;
        for (int i = 0; i < shots; i++) fire(p, s, w, visor);
        if (shots > 0 && p.age % 2 == 0)
            w.playSound(null, p.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_SNARE.value(), SoundCategory.PLAYERS, 0.4f, 1.7f);

        if (s.pressed(InputPayload.ABILITY2) && s.cd[0] == 0) { helix(p, s, w); s.cd[0] = HELIX_COOLDOWN; }
        if (s.pressed(InputPayload.ULT) && s.ultPoints >= ultCost()) {
            s.ultPoints = 0; s.timer = VISOR_TICKS;
            w.playSound(null, p.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.8f, 1.5f);
        }
    }

    private void fire(ServerPlayerEntity p, State s, ServerWorld w, boolean visor) {
        Vec3d eye = p.getEyePos();
        if (visor) {
            LivingEntity t = visorTarget(p);
            if (t != null) {                                    // 조준경: 시야 안의 적을 자동으로 맞춘다
                Vec3d c = t.getBoundingBox().getCenter();
                double dealt = com.minewatch.server.OwDamage.deal(p, t, GUN.damageAt(eye.distanceTo(c), false), false, false);
                s.addUlt(this, dealt);
                HeroKit.beam(w, eye.add(p.getRotationVec(1f).multiply(0.8)), c, 1f, 0.8f, 0.2f, 0.7f);
                return;
            }
        }
        // 연사하면 탄퍼짐이 커진다 (0.3° -> 2.5°, 20틱에 걸쳐)
        double spread = visor ? 0 : 0.3 + 2.2 * Math.min(1.0, s.firing / 20.0);
        HeroKit.Hit h = HeroKit.trace(p, GUN.range(), spread, e -> HeroKit.isEnemy(p, e));
        if (h.entity() != null) {
            double dealt = com.minewatch.server.OwDamage.deal(p, h.entity(), GUN.damageAt(h.dist(), h.head()), h.head(), false);
            s.addUlt(this, dealt);
        }
        HeroKit.beam(w, eye.add(p.getRotationVec(1f).multiply(0.8)), h.point(), 0.4f, 0.7f, 1f, 0.5f);
    }

    private LivingEntity visorTarget(ServerPlayerEntity p) {
        LivingEntity best = null;
        double bestAngle = 1e9;
        Vec3d eye = p.getEyePos(), look = p.getRotationVec(1f);
        for (LivingEntity e : HeroKit.inCone(p, VISOR_RANGE, VISOR_CONE, x -> HeroKit.isEnemy(p, x))) {
            if (!p.canSee(e)) continue;
            double ang = Math.acos(Math.min(1, e.getBoundingBox().getCenter().subtract(eye).normalize().dotProduct(look)));
            if (ang < bestAngle) { bestAngle = ang; best = e; }
        }
        return best;
    }

    private void helix(ServerPlayerEntity p, State s, ServerWorld w) {
        HeroKit.Hit h = HeroKit.trace(p, 50, 0, e -> HeroKit.isEnemy(p, e));
        Vec3d at = h.point();
        w.spawnParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        w.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 0.8f, 1.3f);
        for (LivingEntity e : HeroKit.inRadius(w, at, HELIX_RADIUS, x -> HeroKit.isEnemy(p, x))) {
            double d = e.getBoundingBox().getCenter().distanceTo(at);
            double dmg = HELIX_DAMAGE * (1.0 - 0.5 * Math.min(1.0, d / HELIX_RADIUS));
            s.addUlt(this, com.minewatch.server.OwDamage.deal(p, e, dmg, false, true));
        }
    }

    @Override
    public void onDeath(ServerPlayerEntity p, HeroState hs) {
        hs.timer = 0; hs.flag = false; hs.gun.fill(GUN);
    }

    @Override
    public StatePayload toPayload(HeroState s) {
        return payload(s, s.gun.ammo, GUN.maxAmmo(), s.gun.reload > 0, false, s.timer > 0 ? s.timer * 100 / VISOR_TICKS : 0,
                new StatePayload.Slot("sprint", 0, 0, 0, s.flag),
                new StatePayload.Slot("helix", frac(s.cd[0], HELIX_COOLDOWN), 0, 0, false));
    }
}
