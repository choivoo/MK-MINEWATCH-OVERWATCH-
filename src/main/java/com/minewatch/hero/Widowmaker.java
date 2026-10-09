package com.minewatch.hero;

import com.minewatch.ModItems;
import com.minewatch.net.InputPayload;
import com.minewatch.net.StatePayload;
import com.minewatch.server.BotManager;
import com.minewatch.server.OwDamage;
import com.minewatch.server.VenomMines;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/** 위도우메이커: 저격 소총(우클릭 조준) / 갈고리(Shift) / 독 지뢰(E) / 적외선 투시(Q). */
public class Widowmaker extends Hero {
    public static final GunSpec SMG = new GunSpec(30, 40, 12, 2.0, 20, 30, 0.5, 60, 10, 1);
    public static final double SNIPER_BODY = 12, SNIPER_FULL = 120, SNIPER_HEAD_MULT = 2.5, SNIPER_RANGE = 200;
    public static final int CHARGE_TICKS = 27, SNIPER_COOLDOWN = 20;
    public static final int HOOK_COOLDOWN = 200, MINE_COOLDOWN = 300, VISION_TICKS = 200;
    public static final double HOOK_RANGE = 22;

    static final class State extends HeroState {
        double charge;                 // 0~1 조준 충전
        Vec3d hookDest; int hookTicks;
    }

    public Widowmaker() { super(3, "widowmaker"); }

    @Override public Role role() { return Role.DAMAGE; }
    @Override public double maxHealthOw() { return 200; }
    @Override public double ultCost() { return 1600; }
    @Override public Item weapon() { return ModItems.SNIPER_RIFLE; }
    @Override public HeroState createState() { return new State(); }
    @Override public void onSelect(ServerPlayerEntity p, HeroState hs) { hs.gun.fill(SMG); }

    @Override
    public void tick(ServerPlayerEntity p, HeroState hs) {
        State s = (State) hs;
        if (!prelude(s)) return;
        ServerWorld w = p.getServerWorld();
        boolean holding = p.getMainHandStack().getItem() == weapon();

        // 갈고리로 이동 중
        if (s.hookTicks > 0 && s.hookDest != null) {
            Vec3d to = s.hookDest.subtract(p.getPos());
            boolean moved = HeroKit.moveToward(p, p.getPos().add(to.multiply(1.0 / s.hookTicks)));
            if (--s.hookTicks == 0 || !moved || to.length() < 0.8) { s.hookTicks = 0; s.hookDest = null; }
        }

        if (holding && s.pressed(InputPayload.ALT_FIRE)) s.flag = !s.flag;     // 조준 토글
        if (!holding) s.flag = false;
        boolean scoped = s.flag;
        if (scoped) {
            s.charge = Math.min(1.0, s.charge + 1.0 / CHARGE_TICKS);
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 3, 1, false, false, false));
        } else s.charge = 0;

        s.gun.tickReload(SMG);
        if (holding && (s.pressed(InputPayload.RELOAD) || s.gun.ammo == 0)) s.gun.startReload(SMG);

        if (scoped) {
            // 저격: 충전량에 따라 12~120, 헤드샷 x2.5
            if (holding && s.held(InputPayload.FIRE) && s.cd[2] == 0 && s.gun.reload == 0 && s.gun.ammo > 0) {
                s.gun.ammo--;
                s.cd[2] = SNIPER_COOLDOWN;
                double base = SNIPER_BODY + (SNIPER_FULL - SNIPER_BODY) * s.charge;
                HeroKit.Hit h = HeroKit.trace(p, SNIPER_RANGE, 0, e -> HeroKit.isEnemy(p, e));
                if (h.entity() != null)
                    s.addUlt(this, OwDamage.deal(p, h.entity(), base * (h.head() ? SNIPER_HEAD_MULT : 1.0), h.head(), false));
                HeroKit.beam(w, p.getEyePos().add(p.getRotationVec(1f).multiply(0.8)), h.point(), 1f, 0.3f, 0.9f, 0.8f);
                w.playSound(null, p.getBlockPos(), SoundEvents.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.6f);
                s.charge = 0;
            }
        } else {
            boolean canFire = holding && s.held(InputPayload.FIRE) && s.gun.reload == 0;
            int shots = s.gun.shots(SMG, canFire);
            for (int i = 0; i < shots; i++) {
                LivingEntity hit = HeroKit.shoot(p, s, this, SMG, 2.5, 1.0);
                if (hit == null && p.age % 4 == 0) { /* 빗나감 */ }
            }
            if (shots > 0 && p.age % 2 == 0)
                w.playSound(null, p.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_SNARE.value(), SoundCategory.PLAYERS, 0.35f, 1.9f);
        }

        if (s.pressed(InputPayload.ABILITY1) && s.cd[0] == 0) grapple(p, s, w);
        if (s.pressed(InputPayload.ABILITY2) && s.cd[1] == 0 && placeMine(p, w)) s.cd[1] = MINE_COOLDOWN;

        if (s.pressed(InputPayload.ULT) && s.ultPoints >= ultCost()) { s.ultPoints = 0; s.timer = VISION_TICKS; }
        if (s.timer > 0) {
            s.timer--;
            if (p.age % 10 == 0) {
                for (LivingEntity e : enemies(p, w))
                    e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 25, 0, false, false, false));
            }
        }
    }

    private static java.util.List<LivingEntity> enemies(ServerPlayerEntity p, ServerWorld w) {
        java.util.List<LivingEntity> out = new java.util.ArrayList<>();
        for (LivingEntity e : w.getPlayers()) if (HeroKit.isEnemy(p, e)) out.add(e);
        for (LivingEntity e : BotManager.all()) if (HeroKit.isEnemy(p, e)) out.add(e);
        return out;
    }

    private BlockHitResult blockAim(ServerPlayerEntity p, double range) {
        Vec3d eye = p.getEyePos();
        return p.getServerWorld().raycast(new RaycastContext(eye, eye.add(p.getRotationVec(1f).multiply(range)),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
    }

    private void grapple(ServerPlayerEntity p, State s, ServerWorld w) {
        BlockHitResult bh = blockAim(p, HOOK_RANGE);
        if (bh.getType() == HitResult.Type.MISS) return;
        // 맞은 면에서 살짝 떨어진 곳(발이 닿을 높이)으로 이동
        Vec3d dest = bh.getPos().add(Vec3d.of(bh.getSide().getVector()).multiply(0.6)).add(0, 0.1, 0);
        s.hookDest = dest; s.hookTicks = 6; s.cd[0] = HOOK_COOLDOWN;
        HeroKit.beam(w, p.getEyePos(), bh.getPos(), 0.6f, 0.6f, 0.6f, 0.6f);
        w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_FISHING_BOBBER_THROW, SoundCategory.PLAYERS, 0.8f, 1.2f);
    }

    private boolean placeMine(ServerPlayerEntity p, ServerWorld w) {
        BlockHitResult bh = blockAim(p, 20);
        if (bh.getType() == HitResult.Type.MISS) return false;
        Vec3d at = bh.getPos().add(Vec3d.of(bh.getSide().getVector()).multiply(0.1));
        VenomMines.place(w, at, p);
        w.playSound(null, p.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_PLACE, SoundCategory.PLAYERS, 0.8f, 0.8f);
        return true;
    }

    @Override
    public void onDeath(ServerPlayerEntity p, HeroState hs) {
        State s = (State) hs;
        s.flag = false; s.charge = 0; s.hookTicks = 0; s.timer = 0; s.gun.fill(SMG);
    }

    @Override
    public StatePayload toPayload(HeroState hs) {
        State s = (State) hs;
        return payload(s, s.gun.ammo, SMG.maxAmmo(), s.gun.reload > 0, s.flag, (int) (s.charge * 100),
                new StatePayload.Slot("grapple", frac(s.cd[0], HOOK_COOLDOWN), 0, 0, s.hookTicks > 0),
                new StatePayload.Slot("venom", frac(s.cd[1], MINE_COOLDOWN), 0, 0, false));
    }
}
