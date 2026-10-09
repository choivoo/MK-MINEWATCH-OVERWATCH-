package com.minewatch.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** 서버 -> 공격자: 힛마커. kind: 0 명중, 1 헤드샷, 2 처치. */
public record HitPayload(int kind) implements CustomPayload {
    public static final Id<HitPayload> ID = new Id<>(Identifier.of("minewatch", "hit"));
    public static final PacketCodec<RegistryByteBuf, HitPayload> CODEC = PacketCodec.of(
            (v, b) -> b.writeByte(v.kind), b -> new HitPayload(b.readByte()));

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
