package com.minewatch.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** 클라이언트 -> 서버: 영웅 선택 요청. heroId 가 빈 문자열이면 해제. */
public record SelectHeroPayload(String heroId) implements CustomPayload {
    public static final Id<SelectHeroPayload> ID = new Id<>(Identifier.of("minewatch", "select_hero"));
    public static final PacketCodec<RegistryByteBuf, SelectHeroPayload> CODEC = PacketCodec.of(
            (v, b) -> b.writeString(v.heroId, 32),
            b -> new SelectHeroPayload(b.readString(32)));

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
