package com.minewatch.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 서버 -> 클라이언트: 퍽 진행 상태.
 * stage: 0 없음, 1 마이너 선택 가능, 2 마이너 선택 완료, 3 메이저 선택 가능, 4 메이저 선택 완료.
 * offerA/offerB 는 지금 고를 수 있는 퍽 id(선택할 수 없으면 빈 문자열).
 */
public record PerkStatePayload(int xp, int next, int stage, String offerA, String offerB, String minor, String major)
        implements CustomPayload {
    public static final Id<PerkStatePayload> ID = new Id<>(Identifier.of("minewatch", "perk_state"));
    public static final PacketCodec<RegistryByteBuf, PerkStatePayload> CODEC = PacketCodec.of(
            (v, b) -> { b.writeVarInt(v.xp); b.writeVarInt(v.next + 1); b.writeVarInt(v.stage);
                        b.writeString(v.offerA, 48); b.writeString(v.offerB, 48); b.writeString(v.minor, 48); b.writeString(v.major, 48); },
            b -> new PerkStatePayload(b.readVarInt(), b.readVarInt() - 1, b.readVarInt(),
                    b.readString(48), b.readString(48), b.readString(48), b.readString(48)));
    public static final PerkStatePayload NONE = new PerkStatePayload(0, -1, 0, "", "", "", "");

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
