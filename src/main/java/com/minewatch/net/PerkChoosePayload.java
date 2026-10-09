package com.minewatch.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** 클라이언트 -> 서버: 퍽 선택. */
public record PerkChoosePayload(String perkId) implements CustomPayload {
    public static final Id<PerkChoosePayload> ID = new Id<>(Identifier.of("minewatch", "perk_choose"));
    public static final PacketCodec<RegistryByteBuf, PerkChoosePayload> CODEC = PacketCodec.of(
            (v, b) -> b.writeString(v.perkId, 48), b -> new PerkChoosePayload(b.readString(48)));

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
