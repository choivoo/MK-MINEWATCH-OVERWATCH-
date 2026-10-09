package com.minewatch.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** 서버 -> 클라이언트: 킬피드 한 줄. 팀은 0=A, 1=B, -1=없음. */
public record KillFeedPayload(String killer, int killerTeam, String victim, int victimTeam) implements CustomPayload {
    public static final Id<KillFeedPayload> ID = new Id<>(Identifier.of("minewatch", "killfeed"));
    public static final PacketCodec<RegistryByteBuf, KillFeedPayload> CODEC = PacketCodec.of(
            (v, b) -> { b.writeString(v.killer, 64); b.writeVarInt(v.killerTeam + 1); b.writeString(v.victim, 64); b.writeVarInt(v.victimTeam + 1); },
            b -> new KillFeedPayload(b.readString(64), b.readVarInt() - 1, b.readString(64), b.readVarInt() - 1));

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
