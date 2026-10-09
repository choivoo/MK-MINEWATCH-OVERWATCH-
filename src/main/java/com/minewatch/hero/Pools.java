package com.minewatch.hero;

/**
 * 방어구/보호막 풀 (오버워치 단위). 체력은 마인크래프트 체력(Hero.HP_SCALE 배율)을 그대로 쓴다.
 * 피해 순서: 보호막 -> 방어구(피해 감소) -> 체력.
 */
public final class Pools {
    public static final double ARMOR_FLAT_REDUCTION = 5.0;
    public static final int SHIELD_REGEN_DELAY_TICKS = 60;      // 3초
    public static final double SHIELD_REGEN_PER_TICK = 20.0 / 20.0; // 초당 20

    public double armor, maxArmor, shield, maxShield;
    public int shieldDelay;

    public void init(double maxArmor, double maxShield) {
        this.maxArmor = maxArmor; this.maxShield = maxShield;
        refill();
    }

    public void refill() { armor = maxArmor; shield = maxShield; shieldDelay = 0; }

    /** 피해를 흡수하고, 체력에 들어갈 남은 피해를 반환한다. */
    public double absorb(double damage) {
        if (damage <= 0) return 0;
        shieldDelay = SHIELD_REGEN_DELAY_TICKS;
        double s = Math.min(shield, damage);
        shield -= s;
        damage -= s;
        if (damage > 0 && armor > 0) {
            double reduced = Math.max(damage * 0.5, damage - ARMOR_FLAT_REDUCTION);
            if (armor >= reduced) { armor -= reduced; return 0; }
            double left = reduced - armor;
            armor = 0;
            return left;
        }
        return damage;
    }

    public void tick() {
        if (shield >= maxShield) return;
        if (shieldDelay > 0) { shieldDelay--; return; }
        shield = Math.min(maxShield, shield + SHIELD_REGEN_PER_TICK);
    }
}
