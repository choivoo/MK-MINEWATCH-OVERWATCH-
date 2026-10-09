package com.minewatch.server;

import com.minewatch.hero.Hero;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public final class OwDamage {
    /**
     * 오버워치 단위 피해를 준다. 무적시간을 제거해 초당 40발도 전부 들어가게 하고,
     * 넉백은 되돌린다. 실제로 준 피해(OW 단위)를 반환.
     */
    public static double deal(ServerPlayerEntity attacker, LivingEntity target, double owDamage, boolean headshot, boolean knockback) {
        if (!target.isAlive()) return 0;
        float before = target.getHealth();
        Vec3d vel = target.getVelocity();
        target.timeUntilRegen = 0;
        DamageSource src = attacker.getDamageSources().playerAttack(attacker);
        target.damage(src, (float) (owDamage / Hero.HP_SCALE));
        target.timeUntilRegen = 0;
        if (!knockback) {
            target.setVelocity(vel);
            target.velocityModified = false;
        }
        double dealt = Math.max(0, before - target.getHealth()) * Hero.HP_SCALE;
        if (dealt > 0) {
            if (headshot) attacker.playSoundToPlayer(SoundEvents.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.4f, 1.6f);
            else if (attacker.age % 3 == 0) attacker.playSoundToPlayer(SoundEvents.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.25f, 1.0f);
        }
        return dealt;
    }
    private OwDamage() {}
}
