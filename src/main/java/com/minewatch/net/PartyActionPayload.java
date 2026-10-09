package com.minewatch.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 클라이언트 -> 서버: 파티 화면 동작.
 * action: "start"(mode, target), "stop", "arena", "team"(team: -1 자동, 0 A, 1 B).
 */
public record PartyActionPayload(String action, int mode, int target, int team) implements CustomPayload {
    public static final Id<PartyActionPayload> ID = new Id<>(Identifier.of("minewatch", "party_action"));
    public static final PacketCodec<RegistryByteBuf, PartyActionPayload> CODEC = PacketCodec.of(
            (v, b) -> { b.writeString(v.action, 16); b.writeVarInt(v.mode); b.writeVarInt(v.target); b.writeVarInt(v.team + 1); },
            b -> new PartyActionPayload(b.readString(16), b.readVarInt(), b.readVarInt(), b.readVarInt() - 1));

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
