package com.minewatch.server;

import com.minewatch.hero.Hero;
import com.minewatch.hero.HeroState;
import com.minewatch.net.DamageDirPayload;
import com.minewatch.net.HitPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public final class OwDamage {
    /**
     * 오버워치 단위 피해를 준다. 공격자는 플레이어 또는 봇. 보호막/방어구를 먼저 소모하고,
     * 무적시간을 제거해 초당 40발도 전부 들어가게 하며, 넉백은 되돌린다. 실제로 준 피해(OW 단위)를 반환.
     */
    public static double deal(LivingEntity attacker, LivingEntity target, double owDamage, boolean headshot, boolean knockback) {
        if (!target.isAlive()) return 0;
        if (target != attacker && MatchManager.sameTeam(attacker, target)) return 0;   // 아군 피해 금지
        HeroState hs = null;
        if (target instanceof ServerPlayerEntity tp) {
            hs = HeroManager.stateOf(tp);
            if (hs != null && hs.invulnerable()) return 0;
        }
        // 공격자의 피해 증가 버프
        if (attacker instanceof ServerPlayerEntity ap) {
            HeroState ah = HeroManager.stateOf(ap);
            if (ah != null) owDamage *= ah.outMult();
        }
        double remaining = owDamage, absorbed = 0;
        if (hs != null) {
            // 정면 방벽 -> 받는 피해 감소 -> 보호막/방어구
            double afterBarrier = owDamage;
            if (hs.barrierUp && target instanceof ServerPlayerEntity bt) {
                Vec3d look = bt.getRotationVec(1f);
                afterBarrier = hs.absorbFront(look.x, look.z, attacker.getX() - bt.getX(), attacker.getZ() - bt.getZ(), 70, owDamage);
            }
            double barrierAbsorbed = owDamage - afterBarrier;
            remaining = hs.pools.absorb(afterBarrier * hs.inMult());
            absorbed = barrierAbsorbed + (afterBarrier * hs.inMult() - remaining);
        }
        double toHealth = 0;
        if (remaining > 0) {
            float before = target.getHealth();
            Vec3d vel = target.getVelocity();
            target.timeUntilRegen = 0;
            DamageSource src = attacker instanceof PlayerEntity p
                    ? attacker.getDamageSources().playerAttack(p)
                    : attacker.getDamageSources().mobAttack(attacker);
            target.damage(src, (float) (remaining / Hero.HP_SCALE));
            target.timeUntilRegen = 0;
            if (!knockback) {
                target.setVelocity(vel);
                target.velocityModified = false;
            }
            toHealth = Math.max(0, before - target.getHealth()) * Hero.HP_SCALE;
        }
        double dealt = absorbed + toHealth;
        if (dealt > 0 && attacker instanceof ServerPlayerEntity xp && target != attacker) PerkManager.addXp(xp, dealt);
        if (dealt > 0) {
            if (attacker instanceof ServerPlayerEntity sp) {
                boolean kill = !target.isAlive();
                ServerPlayNetworking.send(sp, new HitPayload(kill ? 2 : headshot ? 1 : 0));
                if (kill) Sfx.play(sp, "kill", 0.7f, 1f);
                else if (headshot) Sfx.play(sp, "hit_crit", 0.6f, 1f);
                else if (sp.age % 3 == 0) Sfx.play(sp, "hit", 0.5f, 1f);
            }
            if (target instanceof ServerPlayerEntity tp && tp != attacker)
                ServerPlayNetworking.send(tp, new DamageDirPayload(attacker.getX(), attacker.getZ()));
        }
        return dealt;
    }
    private OwDamage() {}
}
