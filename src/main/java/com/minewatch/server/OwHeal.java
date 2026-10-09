package com.minewatch.server;

import com.minewatch.hero.Hero;
import net.minecraft.server.network.ServerPlayerEntity;

public final class OwHeal {
    /** 체력을 오버워치 단위로 회복한다. 실제로 회복된 양(OW)을 반환. */
    public static double heal(ServerPlayerEntity target, double owAmount) {
        if (!target.isAlive() || owAmount <= 0) return 0;
        float before = target.getHealth();
        target.setHealth(Math.min(target.getMaxHealth(), before + (float) (owAmount / Hero.HP_SCALE)));
        return (target.getHealth() - before) * Hero.HP_SCALE;
    }
    private OwHeal() {}
}
