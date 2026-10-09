package com.minewatch.hero;

import com.minewatch.ModItems;
import com.minewatch.net.InputPayload;
import com.minewatch.net.StatePayload;
import com.minewatch.server.HeroManager;
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
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/** 아나: 생체 소총(우클릭 조준) / 생체 수류탄(Shift) / 수면총(E) / 나노 강화제(Q). */
public class Ana extends Hero {
    public static final GunSpec GUN = new GunSpec(12, 40, 70, 1.0, 100, 100, 1.0, 80, 1.25, 1);
    public static final double HEAL_SHOT = 75, SLEEP_DAMAGE = 5;
    public static final int SLEEP_TICKS = 100, SLEEP_COOLDOWN = 280;
    public static final int GRENADE_COOLDOWN = 200;
    public static final double GRENADE_RANGE = 35, GRENADE_RADIUS = 4.5, GRENADE_HEAL = 100, GRENADE_DAMAGE = 60;
    public static final int NANO_TICKS = 160;
    public static final double NANO_RANGE = 40;

    public Ana() { super(6, "ana"); }

    @Override public Role role() { return Role.SUPPORT; }
    @Override public double maxHealthOw() { return 250; }
    @Override public double ultCost() { return 2000; }
    @Override public Item weapon() { return ModItems.BIOTIC_RIFLE; }
    @Override public HeroState createState() { return new HeroState(); }
    @Override public void onSelect(ServerPlayerEntity p, HeroState hs) { hs.gun.fill(GUN); }

    @Override
    public void tick(ServerPlayerEntity p, HeroState s) {
        if (!prelude(s)) return;
        ServerWorld w = p.getServerWorld();
        boolean holding = p.getMainHandStack().getItem() == weapon();

        if (holding && s.pressed(InputPayload.ALT_FIRE)) s.flag = !s.flag;           // 조준 토글
        if (!holding) s.flag = false;
        if (s.flag) p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 3, 1, false, false, false));

        s.gun.tickReload(GUN);
        if (holding && (s.pressed(InputPayload.RELOAD) || s.gun.ammo == 0)) s.gun.startReload(GUN, s.reloadSpeed);
        int shots = s.gun.shots(GUN, holding && s.held(InputPayload.FIRE) && s.gun.reload == 0);
        for (int i = 0; i < shots; i++) shoot(p, s, w);

        if (s.pressed(InputPayload.ABILITY1) && s.cd[0] == 0) { grenade(p, s, w); s.cd[0] = GRENADE_COOLDOWN; }
        if (s.pressed(InputPayload.ABILITY2) && s.cd[1] == 0) { sleep(p, s, w); s.cd[1] = SLEEP_COOLDOWN; }
        if (s.pressed(InputPayload.ULT) && s.ultPoints >= ultCost() && nano(p, w)) s.ultPoints = 0;
    }

    /** 생체 소총: 아군이면 치유, 적이면 피해. */
    private void shoot(ServerPlayerEntity p, HeroState s, ServerWorld w) {
        HeroKit.Hit h = HeroKit.trace(p, GUN.range(), 0, e -> HeroKit.isAlly(p, e) || HeroKit.isEnemy(p, e));
        Vec3d from = p.getEyePos().add(p.getRotationVec(1f).multiply(0.8));
        w.playSound(null, p.getBlockPos(), SoundEvents.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 0.7f, 1.4f);
        LivingEntity t = h.entity();
        if (t == null) { HeroKit.beam(w, from, h.point(), 0.6f, 0.4f, 1f, 0.6f); return; }
        if (HeroKit.isAlly(p, t)) {
            double healed = HeroKit.heal(t, HEAL_SHOT * s.healOut());
            s.addUlt(this, healed * 0.5); com.minewatch.server.PerkManager.addXp(p, healed);
            HeroKit.beam(w, from, h.point(), 0.3f, 1f, 0.5f, 0.8f);
        } else {
            s.addUlt(this, OwDamage.deal(p, t, GUN.damage(), false, false));
            HeroKit.beam(w, from, h.point(), 0.8f, 0.3f, 1f, 0.8f);
        }
    }

    private void sleep(ServerPlayerEntity p, HeroState s, ServerWorld w) {
        HeroKit.Hit h = HeroKit.trace(p, 50, 0, e -> HeroKit.isEnemy(p, e));
        HeroKit.beam(w, p.getEyePos().add(p.getRotationVec(1f).multiply(0.8)), h.point(), 0.5f, 0.5f, 1f, 0.6f);
        w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 0.8f, 1.6f);
        LivingEntity t = h.entity();
        if (t == null) return;
        HeroKit.stun(t, SLEEP_TICKS);
        w.spawnParticles(ParticleTypes.NOTE, t.getX(), t.getEyeY() + 0.4, t.getZ(), 3, 0.2, 0.1, 0.2, 0);
        s.addUlt(this, OwDamage.deal(p, t, SLEEP_DAMAGE, false, false));
    }

    /** 생체 수류탄: 조준한 지점 반경 안에서 아군은 치유, 적은 피해. */
    private void grenade(ServerPlayerEntity p, HeroState s, ServerWorld w) {
        Vec3d at = HeroKit.trace(p, GRENADE_RANGE, 0, e -> false).point();
        w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y + 0.3, at.z, 25, GRENADE_RADIUS / 2, 0.4, GRENADE_RADIUS / 2, 0);
        w.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 0.6f);
        for (LivingEntity e : HeroKit.inRadius(w, at, GRENADE_RADIUS, x -> HeroKit.isAlly(p, x) || x == p || HeroKit.isEnemy(p, x))) {
            if (e == p || HeroKit.isAlly(p, e)) s.addUlt(this, HeroKit.heal(e, GRENADE_HEAL * s.healOut()) * 0.5);
            else s.addUlt(this, OwDamage.deal(p, e, GRENADE_DAMAGE, false, false));
        }
    }

    /** 나노 강화제: 조준한 아군에게 공격력 +50%, 받는 피해 -50% (플레이어 영웅만 수치가 적용됨). 대상이 없으면 소모하지 않는다. */
    private boolean nano(ServerPlayerEntity p, ServerWorld w) {
        HeroKit.Hit h = HeroKit.trace(p, NANO_RANGE, 0, e -> HeroKit.isAlly(p, e));
        LivingEntity t = h.entity();
        if (t == null) { p.sendMessage(Text.literal("강화할 아군이 없습니다."), true); return false; }
        if (t instanceof ServerPlayerEntity tp) {
            HeroState ths = HeroManager.stateOf(tp);
            if (ths != null) { ths.boostOut(0.5, NANO_TICKS); ths.resist(0.5, NANO_TICKS); }
        }
        t.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, NANO_TICKS, 0, false, false, false));
        t.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, NANO_TICKS, 1, false, false, false));
        HeroKit.beam(w, p.getEyePos(), t.getBoundingBox().getCenter(), 1f, 0.8f, 0.2f, 1.2f);
        w.playSound(null, p.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.2f);
        return true;
    }

    @Override public void onDeath(ServerPlayerEntity p, HeroState s) { s.flag = false; s.gun.fill(GUN); }

    @Override
    public StatePayload toPayload(HeroState s) {
        return payload(s, s.gun.ammo, GUN.maxAmmo(), s.gun.reload > 0, s.flag, 0,
                new StatePayload.Slot("grenade", frac(s.cd[0], GRENADE_COOLDOWN), 0, 0, false),
                new StatePayload.Slot("sleep", frac(s.cd[1], SLEEP_COOLDOWN), 0, 0, false));
    }
}
