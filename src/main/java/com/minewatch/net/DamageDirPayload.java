package com.minewatch.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** 서버 -> 피해자: 피해를 준 공격자의 위치(수평). 클라이언트가 화면 방향 표시를 그린다. */
public record DamageDirPayload(double x, double z) implements CustomPayload {
    public static final Id<DamageDirPayload> ID = new Id<>(Identifier.of("minewatch", "damage_dir"));
    public static final PacketCodec<RegistryByteBuf, DamageDirPayload> CODEC = PacketCodec.of(
            (v, b) -> { b.writeDouble(v.x); b.writeDouble(v.z); },
            b -> new DamageDirPayload(b.readDouble(), b.readDouble()));

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
