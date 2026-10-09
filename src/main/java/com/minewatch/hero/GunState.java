package com.minewatch.hero;

/** 총기 런타임 상태(순수 로직): 탄약, 장전, 발사 속도 누적. */
public final class GunState {
    public int ammo, reload, max;
    /** 발사 가능 누적치. 1 이상이면 한 발 쏠 수 있다. 유휴 중에도 최대 1발분까지 쌓인다. */
    public double acc;

    public void fill(GunSpec g) { ammo = g.maxAmmo(); max = g.maxAmmo(); reload = 0; acc = 1; }

    /** 즉시 재장전(처치 퍽 등). */
    public void refill() { ammo = max; reload = 0; }

    /** 장전 진행. 이번 틱에 장전이 끝났으면 true. */
    public boolean tickReload(GunSpec g) {
        if (reload <= 0) return false;
        if (--reload == 0) { ammo = g.maxAmmo(); return true; }
        return false;
    }

    public void startReload(GunSpec g) { startReload(g, 1.0); }

    /** speed 배율만큼 빨리 장전한다(퍽). */
    public void startReload(GunSpec g, double speed) {
        if (reload == 0 && ammo < g.maxAmmo()) reload = Math.max(1, (int) Math.round(g.reloadTicks() / Math.max(0.1, speed)));
    }

    /** 이번 틱에 쏠 수 있는 발수(남은 탄약 한도). canFire=false 면 누적만 한다. */
    public int shots(GunSpec g, boolean canFire) {
        acc = Math.min(acc + g.shotsPerTick(), 1 + g.shotsPerTick());
        int n = 0;
        while (canFire && acc >= 1 && ammo > 0) { acc -= 1; ammo--; n++; }
        return n;
    }
}
