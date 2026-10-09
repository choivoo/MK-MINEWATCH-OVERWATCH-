package com.minewatch.hero;

import java.util.ArrayList;
import java.util.List;

/**
 * 영웅별 퍽 정의(오버워치 2 방식: 마이너 2택1, 메이저 2택1). 효과는 수치 보정(Mod)으로 표현한다.
 * 퍽 id 는 "영웅:키" 형식이며, 이름/설명은 언어 파일 perk.minewatch.<키>(.desc) 에서 가져온다.
 */
public final class Perks {
    public enum Fx {
        DAMAGE, RELOAD_SPEED, HEAL, ULT_GAIN, SPEED_AMP, ARMOR, MAX_HEALTH, BARRIER,
        COOLDOWN_0, COOLDOWN_1, KILL_RELOAD, HEALPACK_BLINK, RECALL_FULL_HEAL
    }

    public record Mod(Fx fx, double value) {}

    public record Perk(String hero, String key, int tier, List<Mod> mods) {
        public String id() { return hero + ":" + key; }
    }

    private static final List<Perk> ALL = new ArrayList<>();

    private static void add(String hero, String key, int tier, Mod... mods) { ALL.add(new Perk(hero, key, tier, List.of(mods))); }
    private static Mod m(Fx fx, double v) { return new Mod(fx, v); }

    static {
        // 트레이서 (Seafle 에서 가져온 4종)
        add("tracer", "blink_packs", 0, m(Fx.HEALPACK_BLINK, 1));
        add("tracer", "kinetic_reload", 0, m(Fx.KILL_RELOAD, 1));
        add("tracer", "temporal_regen", 1, m(Fx.RECALL_FULL_HEAL, 1));
        add("tracer", "quantum_entanglement", 1, m(Fx.COOLDOWN_1, 1.33));

        add("soldier76", "quick_reload", 0, m(Fx.RELOAD_SPEED, 1.3));
        add("soldier76", "sprint_boost", 0, m(Fx.SPEED_AMP, 1));
        add("soldier76", "cd_primary", 1, m(Fx.COOLDOWN_0, 1.3));
        add("soldier76", "ult_gain", 1, m(Fx.ULT_GAIN, 1.25));

        add("widowmaker", "quick_reload", 0, m(Fx.RELOAD_SPEED, 1.3));
        add("widowmaker", "damage_up", 0, m(Fx.DAMAGE, 1.08));
        add("widowmaker", "cd_primary", 1, m(Fx.COOLDOWN_0, 1.3));
        add("widowmaker", "cd_secondary", 1, m(Fx.COOLDOWN_1, 1.3));

        add("reinhardt", "tough_armor", 0, m(Fx.ARMOR, 50));
        add("reinhardt", "strong_barrier", 0, m(Fx.BARRIER, 0.2));
        add("reinhardt", "cd_primary", 1, m(Fx.COOLDOWN_0, 1.3));
        add("reinhardt", "ult_gain", 1, m(Fx.ULT_GAIN, 1.25));

        add("roadhog", "quick_reload", 0, m(Fx.RELOAD_SPEED, 1.3));
        add("roadhog", "thick_skin", 0, m(Fx.MAX_HEALTH, 100));
        add("roadhog", "cd_primary", 1, m(Fx.COOLDOWN_0, 1.3));
        add("roadhog", "cd_secondary", 1, m(Fx.COOLDOWN_1, 1.3));

        add("ana", "quick_reload", 0, m(Fx.RELOAD_SPEED, 1.3));
        add("ana", "potent_heal", 0, m(Fx.HEAL, 1.1));
        add("ana", "cd_primary", 1, m(Fx.COOLDOWN_0, 1.3));
        add("ana", "ult_gain", 1, m(Fx.ULT_GAIN, 1.25));

        add("mercy", "potent_heal", 0, m(Fx.HEAL, 1.1));
        add("mercy", "fleet", 0, m(Fx.SPEED_AMP, 1));
        add("mercy", "cd_primary", 1, m(Fx.COOLDOWN_0, 1.3));
        add("mercy", "ult_gain", 1, m(Fx.ULT_GAIN, 1.25));
    }

    /** 해당 영웅/단계에서 고를 수 있는 퍽(2개). */
    public static List<Perk> offers(String hero, int tier) {
        List<Perk> out = new ArrayList<>();
        for (Perk p : ALL) if (p.hero().equals(hero) && p.tier() == tier) out.add(p);
        return out;
    }

    public static Perk find(String id) {
        for (Perk p : ALL) if (p.id().equals(id)) return p;
        return null;
    }

    public static List<Perk> all() { return List.copyOf(ALL); }

    /** 퍽 수치를 상태에 적용한다(플레이어가 필요 없는 부분). */
    public static void apply(HeroState s, Perk perk) {
        for (Mod mod : perk.mods()) {
            switch (mod.fx()) {
                case DAMAGE -> s.dmgMult *= mod.value();
                case RELOAD_SPEED -> s.reloadSpeed *= mod.value();
                case HEAL -> s.healMult *= mod.value();
                case ULT_GAIN -> s.ultGainMult *= mod.value();
                case SPEED_AMP -> s.speedAmp = Math.max(s.speedAmp, (int) mod.value());
                case ARMOR -> { s.pools.maxArmor += mod.value(); s.pools.armor += mod.value(); }
                case BARRIER -> { s.barrierMax *= 1 + mod.value(); s.barrierHp *= 1 + mod.value(); }
                case COOLDOWN_0 -> s.cdRate[0] *= mod.value();
                case COOLDOWN_1 -> s.cdRate[1] *= mod.value();
                case KILL_RELOAD -> s.killReload = true;
                case HEALPACK_BLINK -> s.healpackBlink = true;
                case RECALL_FULL_HEAL -> s.recallFullHeal = true;
                case MAX_HEALTH -> s.bonusMaxHealthOw += mod.value();
            }
        }
    }
    private Perks() {}
}
