package com.minewatch.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** 서버 -> 클라이언트: 방어구/보호막 (오버워치 단위, 반올림). 체력은 바닐라 체력에서 계산. */
public record PoolsPayload(int armor, int maxArmor, int shield, int maxShield) implements CustomPayload {
    public static final Id<PoolsPayload> ID = new Id<>(Identifier.of("minewatch", "pools"));
    public static final PacketCodec<RegistryByteBuf, PoolsPayload> CODEC = PacketCodec.of(
            (v, b) -> { b.writeVarInt(v.armor); b.writeVarInt(v.maxArmor); b.writeVarInt(v.shield); b.writeVarInt(v.maxShield); },
            b -> new PoolsPayload(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt()));
    public static final PoolsPayload NONE = new PoolsPayload(0, 0, 0, 0);

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
