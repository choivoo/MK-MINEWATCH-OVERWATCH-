package com.minewatch.hero;

import com.minewatch.ModItems;
import com.minewatch.net.InputPayload;
import com.minewatch.net.StatePayload;
import com.minewatch.server.HeroManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/** 메르시: 카듀세우스 지팡이(좌클릭 치유, 우클릭 공격력 증폭) / 수호천사(Shift) / 발키리(Q). */
public class Mercy extends Hero {
    public static final double HEAL_PER_TICK = 55.0 / 20.0, BOOST = 0.3;
    public static final double BEAM_RANGE = 15, VALKYRIE_RANGE = 25, BEAM_CONE = 12;
    public static final int GUARDIAN_COOLDOWN = 30, GUARDIAN_TICKS = 8;
    public static final double GUARDIAN_RANGE = 40, GUARDIAN_CONE = 20;
    public static final int VALKYRIE_TICKS = 300;
    public static final double VALKYRIE_HEAL_MULT = 1.3, VALKYRIE_BOOST = 0.3;

    static final class State extends HeroState {
        LivingEntity guardianTarget;
    }

    public Mercy() { super(7, "mercy"); }

    @Override public Role role() { return Role.SUPPORT; }
    @Override public double maxHealthOw() { return 225; }
    @Override public double ultCost() { return 2250; }
    @Override public Item weapon() { return ModItems.CADUCEUS_STAFF; }
    @Override public HeroState createState() { return new State(); }
    @Override public void onSelect(ServerPlayerEntity p, HeroState hs) {}

    /** 시선에 가장 가까운(각도) 아군을 찾는다. */
    static LivingEntity allyInSight(ServerPlayerEntity p, double range, double coneDeg) {
        LivingEntity best = null;
        double bestAng = 1e9;
        Vec3d eye = p.getEyePos(), look = p.getRotationVec(1f);
        for (LivingEntity e : HeroKit.inCone(p, range, coneDeg, x -> HeroKit.isAlly(p, x))) {
            if (!p.canSee(e)) continue;
            double ang = Math.acos(Math.min(1, e.getBoundingBox().getCenter().subtract(eye).normalize().dotProduct(look)));
            if (ang < bestAng) { bestAng = ang; best = e; }
        }
        return best;
    }

    @Override
    public void tick(ServerPlayerEntity p, HeroState hs) {
        State s = (State) hs;
        if (!prelude(s)) return;
        ServerWorld w = p.getServerWorld();
        boolean holding = p.getMainHandStack().getItem() == weapon();
        boolean valkyrie = s.timer > 0;

        if (valkyrie) {
            if (--s.timer == 0) endValkyrie(p);
        }

        // 수호천사: 아군에게 빠르게 날아간다
        if (s.guardianTarget != null) {
            Vec3d to = s.guardianTarget.getPos().subtract(p.getPos());
            if (!s.guardianTarget.isAlive() || to.length() < 2.0 || s.stunTicks > 0) s.guardianTarget = null;
            else HeroKit.moveToward(p, p.getPos().add(to.multiply(Math.min(1.0, 4.0 / Math.max(4.0, to.length())) )));
        }
        if (s.pressed(InputPayload.ABILITY1) && s.cd[0] == 0 && s.guardianTarget == null) {
            LivingEntity t = allyInSight(p, GUARDIAN_RANGE, GUARDIAN_CONE);
            if (t != null) {
                s.guardianTarget = t; s.cd[0] = GUARDIAN_COOLDOWN + (int) (p.distanceTo(t) / 4);
                w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.8f, 1.6f);
            }
        }

        // 빔: 좌클릭 = 치유, 우클릭 = 공격력 증폭
        boolean heal = holding && s.held(InputPayload.FIRE), boost = holding && s.held(InputPayload.ALT_FIRE) && !heal;
        if (heal || boost) {
            LivingEntity t = allyInSight(p, valkyrie ? VALKYRIE_RANGE : BEAM_RANGE, BEAM_CONE);
            Vec3d from = p.getEyePos().add(p.getRotationVec(1f).multiply(0.8));
            if (t != null) {
                Vec3d to = t.getBoundingBox().getCenter();
                if (heal) {
                    double healed = HeroKit.heal(t, HEAL_PER_TICK * (valkyrie ? VALKYRIE_HEAL_MULT : 1.0));
                    s.addUlt(this, healed * 0.5);
                    HeroKit.beam(w, from, to, 1f, 0.9f, 0.3f, 0.8f);
                } else {
                    double amt = valkyrie ? VALKYRIE_BOOST + 0.1 : BOOST;
                    if (t instanceof ServerPlayerEntity tp) {
                        HeroState ths = HeroManager.stateOf(tp);
                        if (ths != null) ths.boostOut(amt, 6);
                    }
                    HeroKit.beam(w, from, to, 1f, 0.45f, 0.2f, 0.8f);
                }
            }
        }

        if (s.pressed(InputPayload.ULT) && s.ultPoints >= ultCost() && !valkyrie) {
            s.ultPoints = 0; s.timer = VALKYRIE_TICKS;
            p.getAbilities().allowFlying = true;
            p.sendAbilitiesUpdate();
            w.playSound(null, p.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.8f);
        }
    }

    private void endValkyrie(ServerPlayerEntity p) {
        net.minecraft.world.GameMode gm = p.interactionManager.getGameMode();
        if (!gm.isCreative() && gm != net.minecraft.world.GameMode.SPECTATOR) {
            p.getAbilities().allowFlying = false;
            p.getAbilities().flying = false;
            p.sendAbilitiesUpdate();
        }
    }

    @Override
    public void onDeselect(ServerPlayerEntity p, HeroState hs) { if (hs.timer > 0) { hs.timer = 0; endValkyrie(p); } }

    @Override
    public void onDeath(ServerPlayerEntity p, HeroState hs) {
        State s = (State) hs;
        s.guardianTarget = null;
        if (s.timer > 0) { s.timer = 0; endValkyrie(p); }
    }

    @Override
    public StatePayload toPayload(HeroState hs) {
        State s = (State) hs;
        return payload(s, 0, 0, false, false, s.timer > 0 ? s.timer * 100 / VALKYRIE_TICKS : 0,
                new StatePayload.Slot("guardian", frac(s.cd[0], GUARDIAN_COOLDOWN + 10), 0, 0, s.guardianTarget != null));
    }
}
