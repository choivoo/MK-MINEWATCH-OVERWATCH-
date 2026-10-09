package com.minewatch.server;

import com.minewatch.hero.Hero;
import com.minewatch.hero.HeroState;
import com.minewatch.net.DamageDirPayload;
import com.minewatch.net.HitPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public final class OwDamage {
    /**
     * 오버워치 단위 피해를 준다. 보호막/방어구를 먼저 소모하고, 무적시간을 제거해
     * 초당 40발도 전부 들어가게 하며, 넉백은 되돌린다. 실제로 준 피해(OW 단위)를 반환.
     */
    public static double deal(ServerPlayerEntity attacker, LivingEntity target, double owDamage, boolean headshot, boolean knockback) {
        if (!target.isAlive()) return 0;
        HeroState hs = null;
        if (target instanceof ServerPlayerEntity tp) {
            if (tp != attacker && MatchManager.sameTeam(attacker, tp)) return 0;   // 아군 피해 금지
            hs = HeroManager.stateOf(tp);
            if (hs != null && hs.invulnerable()) return 0;
        }
        double remaining = owDamage, absorbed = 0;
        if (hs != null) {
            remaining = hs.pools.absorb(owDamage);
            absorbed = owDamage - remaining;
        }
        double toHealth = 0;
        if (remaining > 0) {
            float before = target.getHealth();
            Vec3d vel = target.getVelocity();
            target.timeUntilRegen = 0;
            DamageSource src = attacker.getDamageSources().playerAttack(attacker);
            target.damage(src, (float) (remaining / Hero.HP_SCALE));
            target.timeUntilRegen = 0;
            if (!knockback) {
                target.setVelocity(vel);
                target.velocityModified = false;
            }
            toHealth = Math.max(0, before - target.getHealth()) * Hero.HP_SCALE;
        }
        double dealt = absorbed + toHealth;
        if (dealt > 0) {
            boolean kill = !target.isAlive();
            ServerPlayNetworking.send(attacker, new HitPayload(kill ? 2 : headshot ? 1 : 0));
            if (kill) attacker.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5f, 0.6f);
            else if (headshot) attacker.playSoundToPlayer(SoundEvents.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.4f, 1.6f);
            else if (attacker.age % 3 == 0) attacker.playSoundToPlayer(SoundEvents.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.25f, 1.0f);
            if (target instanceof ServerPlayerEntity tp && tp != attacker)
                ServerPlayNetworking.send(tp, new DamageDirPayload(attacker.getX(), attacker.getZ()));
        }
        return dealt;
    }
    private OwDamage() {}
}
