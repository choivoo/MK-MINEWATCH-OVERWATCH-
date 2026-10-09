package com.minewatch.hero;

import com.minewatch.net.StatePayload;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 모든 오버워치 영웅의 기반 클래스. 새 영웅은 이 클래스를 상속하고 HeroRegistry 에 등록하면 된다.
 */
public abstract class Hero {
    /** 오버워치 체력 1 = 마인크래프트 체력 1/HP_SCALE. (150 OW HP = 20 MC HP) */
    public static final double HP_SCALE = 150.0 / 20.0;

    public final int numericId;
    public final String id;

    protected Hero(int numericId, String id) { this.numericId = numericId; this.id = id; }

    public Role role() { return Role.DAMAGE; }
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
}
