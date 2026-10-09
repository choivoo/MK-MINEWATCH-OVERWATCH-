package com.minewatch.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** 서버 -> 클라이언트: 매치 상태(수신자 기준 팀). state: 0 없음, 1 카운트다운, 2 진행, 3 종료. */
public record MatchPayload(int state, int myTeam, int scoreA, int scoreB, int target, int ticksLeft) implements CustomPayload {
    public static final Id<MatchPayload> ID = new Id<>(Identifier.of("minewatch", "match"));
    public static final PacketCodec<RegistryByteBuf, MatchPayload> CODEC = PacketCodec.of(
            (v, b) -> { b.writeVarInt(v.state); b.writeVarInt(v.myTeam + 1); b.writeVarInt(v.scoreA);
                        b.writeVarInt(v.scoreB); b.writeVarInt(v.target); b.writeVarInt(v.ticksLeft); },
            b -> new MatchPayload(b.readVarInt(), b.readVarInt() - 1, b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt()));
    public static final MatchPayload NONE = new MatchPayload(0, -1, 0, 0, 0, 0);

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
