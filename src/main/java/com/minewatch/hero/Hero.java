package com.minewatch.hero;

import com.minewatch.net.StatePayload;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 모든 오버워치 영웅의 기반 클래스. 새 영웅은 이 클래스를 상속하고 HeroRegistry 에 등록하면 된다.
 * 영웅 = 역할 + 체력/방어구/보호막 + 무기 아이템 + 능력(Shift, E) + 궁극기(Q).
 */
public abstract class Hero {
    /** 오버워치 체력 1 = 마인크래프트 체력 1/HP_SCALE. (150 OW HP = 20 MC HP) */
    public static final double HP_SCALE = 150.0 / 20.0;

    public final int numericId;
    public final String id;

    protected Hero(int numericId, String id) { this.numericId = numericId; this.id = id; }

    public Role role() { return Role.DAMAGE; }
    /** 오버워치 단위 최대 체력/방어구/보호막. */
    public double maxHealthOw() { return 150; }
    public double maxArmor() { return 0; }
    public double maxShield() { return 0; }
    /** 이 영웅의 무기 아이템. 선택하면 1번 슬롯에 지급된다. */
    public Item weapon() { return null; }

    public abstract double ultCost();
    /** 초당 자연 궁극기 충전량. */
    public double passiveUltPerSecond() { return 5.0; }
    public abstract HeroState createState();
    public abstract void onSelect(ServerPlayerEntity player, HeroState state);
    public abstract void tick(ServerPlayerEntity player, HeroState state);
    public abstract StatePayload toPayload(HeroState state);
    /** 이 영웅이 플레이어 사망/해제 시 정리할 것이 있으면 구현. */
    public void onDeselect(ServerPlayerEntity player, HeroState state) {}
    public void onDeath(ServerPlayerEntity player, HeroState state) {}

    /**
     * 공통 틱 준비: 쿨다운/버프 감소, 자연 궁극기 충전.
     * 기절 중이면 입력을 비우고 false 를 반환(영웅 로직을 건너뛰어도 됨).
     */
    protected boolean prelude(HeroState s) {
        s.tickEffects();
        s.addUlt(this, passiveUltPerSecond() / 20.0);
        if (s.stunTicks > 0) { s.input = com.minewatch.net.InputPayload.EMPTY; return false; }
        return true;
    }

    /** HUD 페이로드 생성 도우미. */
    protected StatePayload payload(HeroState s, int ammo, int maxAmmo, boolean reloading, boolean scoped, int extra,
                                   StatePayload.Slot... slots) {
        return new StatePayload(numericId, ammo, maxAmmo, reloading, (float) (s.ultPoints / ultCost()), scoped, extra, java.util.List.of(slots));
    }

    /** 재사용 대기 틱 -> 남은 비율. */
    protected static float frac(int ticks, int total) { return total <= 0 ? 0f : Math.min(1f, ticks / (float) total); }
}
