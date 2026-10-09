package com.minewatch.hero;

import com.minewatch.net.InputPayload;

/** 영웅 종류와 무관한 공통 상태. 영웅별 상태는 이를 상속하거나 아래 범용 필드를 쓴다. */
public class HeroState {
    public InputPayload input = InputPayload.EMPTY;
    public InputPayload prevInput = InputPayload.EMPTY;
    /** 궁극기 포인트 (0 ~ hero.ultCost()). */
    public double ultPoints = 0;
    public boolean ultReady = false;
    public final Pools pools = new Pools();

    // ---- 범용(트레이서 외 영웅용) ----
    public final GunState gun = new GunState();
    /** 능력 쿨다운(틱). 인덱스는 영웅이 정한다. */
    public final int[] cd = new int[4];
    /** 지속 시간/임시 타이머, 임시 플래그. */
    public int timer;
    public boolean flag;
    public int stunTicks;

    // ---- 퍽 ----
    public final PerkProgress perk = new PerkProgress();
    public double dmgMult = 1, reloadSpeed = 1, healMult = 1, ultGainMult = 1;
    public int speedAmp = -1;                          // -1 이면 이동 속도 퍽 없음
    public final double[] cdRate = {1, 1, 1, 1};       // 능력 쿨다운 감소 속도 배율
    private final double[] cdAcc = new double[4];
    public boolean killReload, healpackBlink, recallFullHeal;
    public double bonusMaxHealthOw;

    // ---- 버프/디버프 ----
    public double outBoost; public int outBoostTicks;     // 주는 피해/치유 증가
    public double inResist; public int inResistTicks;     // 받는 피해 감소

    // ---- 정면 방벽 ----
    public double barrierHp, barrierMax;
    public boolean barrierUp;
    public int barrierBroken;                              // 파괴 후 재사용 대기(틱)

    public boolean pressed(int flag) { return input.has(flag) && !prevInput.has(flag); }
    public boolean held(int flag) { return input.has(flag); }

    /** 무적 상태(리콜 등)면 true. */
    public boolean invulnerable() { return false; }

    /** 버프(나노/증폭)만 반영한 배율. */
    public double outBuff() { return outBoostTicks > 0 ? 1.0 + outBoost : 1.0; }
    /** 주는 피해 배율 = 버프 x 퍽. */
    public double outMult() { return outBuff() * dmgMult; }
    /** 주는 치유 배율 = 버프 x 퍽. */
    public double healOut() { return outBuff() * healMult; }
    public double inMult() { return inResistTicks > 0 ? 1.0 - inResist : 1.0; }

    public void boostOut(double amount, int ticks) {
        if (amount >= outBoost || outBoostTicks <= 0) outBoost = amount;
        outBoostTicks = Math.max(outBoostTicks, ticks);
    }
    public void resist(double amount, int ticks) {
        if (amount >= inResist || inResistTicks <= 0) inResist = amount;
        inResistTicks = Math.max(inResistTicks, ticks);
    }

    /** 매 틱: 쿨다운/버프/기절 타이머 감소. */
    public void tickEffects() {
        for (int i = 0; i < cd.length; i++) {
            if (cd[i] <= 0) { cdAcc[i] = 0; continue; }
            cdAcc[i] += cdRate[i];
            int dec = (int) cdAcc[i];
            cdAcc[i] -= dec;
            cd[i] = Math.max(0, cd[i] - dec);
        }
        if (outBoostTicks > 0) outBoostTicks--;
        if (inResistTicks > 0) inResistTicks--;
        if (stunTicks > 0) stunTicks--;
        if (barrierBroken > 0) barrierBroken--;
    }

    public void addUlt(Hero h, double amount) { ultPoints = Math.min(h.ultCost(), ultPoints + Math.max(0, amount) * ultGainMult); }

    /** 퍽 경험치(주거나 치유한 양, 처치 보너스). */
    public void addXp(double amount) { perk.addXp(amount); }

    /**
     * 정면 방벽이 올라가 있고 공격이 정면(좌우 halfAngleDeg 이내)에서 왔다면 방벽이 흡수한다.
     * (lookX, lookZ) 는 방어자의 수평 시선, (toX, toZ) 는 방어자 -> 공격자 수평 벡터.
     * 흡수하고 남은 피해를 반환한다. 방벽 체력이 0 이 되면 파괴된다.
     */
    public double absorbFront(double lookX, double lookZ, double toX, double toZ, double halfAngleDeg, double damage) {
        if (!barrierUp || barrierHp <= 0 || damage <= 0) return damage;
        double ll = Math.hypot(lookX, lookZ), tl = Math.hypot(toX, toZ);
        if (ll < 1e-6 || tl < 1e-6) return damage;
        double cos = (lookX * toX + lookZ * toZ) / (ll * tl);
        if (cos < Math.cos(Math.toRadians(halfAngleDeg))) return damage;
        double absorbed = Math.min(barrierHp, damage);
        barrierHp -= absorbed;
        if (barrierHp <= 0) { barrierHp = 0; barrierUp = false; barrierBroken = 100; }
        return damage - absorbed;
    }
}
