package com.minewatch.hero;

/** 히트스캔 총기 사양(순수 로직). 피해는 오버워치 단위, 거리는 블록. */
public record GunSpec(int maxAmmo, int reloadTicks, double damage, double headshotMult,
                      double falloffStart, double falloffEnd, double minMult,
                      double range, double shotsPerSecond, int pellets) {

    /** 거리/헤드샷을 반영한 한 발(펠릿)당 피해. 감쇠 구간에서 선형으로 minMult 까지 줄어든다. */
    public double damageAt(double distance, boolean headshot) {
        double m;
        if (distance <= falloffStart) m = 1.0;
        else if (distance >= falloffEnd) m = minMult;
        else m = 1.0 - (1.0 - minMult) * (distance - falloffStart) / (falloffEnd - falloffStart);
        return damage * m * (headshot ? headshotMult : 1.0);
    }

    public double shotsPerTick() { return shotsPerSecond / 20.0; }
}
