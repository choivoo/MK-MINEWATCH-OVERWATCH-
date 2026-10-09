package com.minewatch.hero;

import com.minewatch.ModItems;
import com.minewatch.net.InputPayload;
import com.minewatch.net.StatePayload;
import com.minewatch.server.OwDamage;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/** 로드호그: 스크랩 건 / 숨 돌리기(Shift) / 사슬 갈고리(E) / 돼지 대학살(Q). */
public class Roadhog extends Hero {
    public static final GunSpec GUN = new GunSpec(5, 30, 5, 1.0, 10, 20, 0.3, 40, 1.1, 15);
    public static final GunSpec HOG = new GunSpec(999, 0, 2.5, 1.0, 8, 18, 0.4, 25, 6, 10);
    public static final double GUN_SPREAD = 5.0, HOG_SPREAD = 7.0;
    public static final int BREATHER_COOLDOWN = 160, BREATHER_TICKS = 30;
    public static final double BREATHER_HEAL_PER_TICK = 10;      // 30틱 동안 300
    public static final int HOOK_COOLDOWN = 160, HOOK_STUN = 12;
    public static final double HOOK_RANGE = 22, HOOK_DAMAGE = 30;
    public static final int HOG_TICKS = 160;

    static final class State extends HeroState {
        final GunState hogGun = new GunState();
        int hog;
    }

    public Roadhog() { super(5, "roadhog"); }

    @Override public Role role() { return Role.TANK; }
    @Override public double maxHealthOw() { return 650; }
    @Override public double ultCost() { return 1800; }
    @Override public Item weapon() { return ModItems.SCRAP_GUN; }
    @Override public HeroState createState() { return new State(); }
    @Override public void onSelect(ServerPlayerEntity p, HeroState hs) { hs.gun.fill(GUN); ((State) hs).hogGun.fill(HOG); }

    @Override
    public void tick(ServerPlayerEntity p, HeroState hs) {
        State s = (State) hs;
        if (!prelude(s)) return;
        ServerWorld w = p.getServerWorld();
        boolean holding = p.getMainHandStack().getItem() == weapon();

        if (s.hog > 0) s.hog--;
        boolean hog = s.hog > 0;

        // 숨 돌리기: 일정 시간 치유 + 받는 피해 감소
        if (s.timer > 0) {
            s.timer--;
            double healed = HeroKit.heal(p, BREATHER_HEAL_PER_TICK);
            if (healed > 0 && p.age % 5 == 0) w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1.0, p.getZ(), 2, 0.3, 0.5, 0.3, 0);
        }
        if (s.pressed(InputPayload.ABILITY1) && s.cd[0] == 0 && s.timer == 0) {
            s.timer = BREATHER_TICKS; s.cd[0] = BREATHER_COOLDOWN; s.resist(0.5, BREATHER_TICKS + 10);
            w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0f, 0.7f);
        }

        // 무기
        GunSpec spec = hog ? HOG : GUN;
        GunState gun = hog ? s.hogGun : s.gun;
        if (hog) gun.ammo = HOG.maxAmmo();                        // 돼지 대학살 중에는 탄약 무한
        else {
            gun.tickReload(GUN);
            if (holding && (s.pressed(InputPayload.RELOAD) || gun.ammo == 0)) gun.startReload(GUN);
        }
        boolean canFire = holding && s.held(InputPayload.FIRE) && (hog || gun.reload == 0);
        int shots = gun.shots(spec, canFire);
        for (int i = 0; i < shots; i++) fire(p, s, w, spec, hog ? HOG_SPREAD : GUN_SPREAD);
        if (shots > 0) w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, hog ? 0.3f : 0.6f, 1.6f);

        if (s.pressed(InputPayload.ABILITY2) && s.cd[1] == 0) { hook(p, s, w); s.cd[1] = HOOK_COOLDOWN; }
        if (s.pressed(InputPayload.ULT) && s.ultPoints >= ultCost()) { s.ultPoints = 0; s.hog = HOG_TICKS; }
    }

    /** 산탄: 펠릿을 모두 쏴서 대상별로 합산한 뒤 한 번에 피해를 준다. */
    private void fire(ServerPlayerEntity p, State s, ServerWorld w, GunSpec spec, double spread) {
        Map<LivingEntity, Double> total = new LinkedHashMap<>();
        Vec3d muzzle = p.getEyePos().add(p.getRotationVec(1f).multiply(0.8));
        for (int i = 0; i < spec.pellets(); i++) {
            HeroKit.Hit h = HeroKit.trace(p, spec.range(), spread, e -> HeroKit.isEnemy(p, e));
            if (h.entity() != null) total.merge(h.entity(), spec.damageAt(h.dist(), false), Double::sum);
            if (i % 5 == 0) HeroKit.beam(w, muzzle, h.point(), 0.8f, 0.6f, 0.3f, 0.5f);
        }
        for (var e : total.entrySet()) s.addUlt(this, OwDamage.deal(p, e.getKey(), e.getValue(), false, false));
    }

    private void hook(ServerPlayerEntity p, State s, ServerWorld w) {
        HeroKit.Hit h = HeroKit.trace(p, HOOK_RANGE, 0, e -> HeroKit.isEnemy(p, e));
        HeroKit.beam(w, p.getEyePos().add(p.getRotationVec(1f).multiply(0.8)), h.point(), 0.5f, 0.5f, 0.5f, 0.7f);
        w.playSound(null, p.getBlockPos(), SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.PLAYERS, 1.0f, 0.8f);
        LivingEntity t = h.entity();
        if (t == null) return;
        Vec3d look = p.getRotationVec(1f);
        Vec3d dest = new Vec3d(p.getX() + look.x * 1.8, t.getY(), p.getZ() + look.z * 1.8);
        HeroKit.moveToward(t, dest);                                    // 앞으로 끌어온다
        HeroKit.stun(t, HOOK_STUN);
        s.addUlt(this, OwDamage.deal(p, t, HOOK_DAMAGE, false, false));
    }

    @Override
    public void onDeath(ServerPlayerEntity p, HeroState hs) {
        State s = (State) hs;
        s.hog = 0; s.timer = 0; s.gun.fill(GUN);
    }

    @Override
    public StatePayload toPayload(HeroState hs) {
        State s = (State) hs;
        return payload(s, s.hog > 0 ? 0 : s.gun.ammo, s.hog > 0 ? 0 : GUN.maxAmmo(), s.gun.reload > 0, false, s.hog > 0 ? s.hog * 100 / HOG_TICKS : 0,
                new StatePayload.Slot("breather", frac(s.cd[0], BREATHER_COOLDOWN), 0, 0, s.timer > 0),
                new StatePayload.Slot("hook", frac(s.cd[1], HOOK_COOLDOWN), 0, 0, false));
    }
}
