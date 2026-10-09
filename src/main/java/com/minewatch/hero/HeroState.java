package com.minewatch.hero;

import com.minewatch.net.InputPayload;

/** 영웅 종류와 무관한 공통 상태. 영웅별 상태는 이를 상속한다. */
public class HeroState {
    public InputPayload input = InputPayload.EMPTY;
    public InputPayload prevInput = InputPayload.EMPTY;
    /** 궁극기 포인트 (0 ~ hero.ultCost()). */
    public double ultPoints = 0;
    public boolean ultReady = false;
    public final Pools pools = new Pools();

    /** 무적 상태(리콜 등)면 true. */
    public boolean invulnerable() { return false; }

    public boolean pressed(int flag) { return input.has(flag) && !prevInput.has(flag); }
    public boolean held(int flag) { return input.has(flag); }
}
