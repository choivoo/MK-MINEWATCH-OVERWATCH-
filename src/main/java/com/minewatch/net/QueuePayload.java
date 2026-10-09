package com.minewatch.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** 서버 -> 클라이언트: 온라인 매치 큐 상태. seconds 는 시작까지 남은 초(없으면 -1). */
public record QueuePayload(boolean inQueue, int queued, int min, int seconds) implements CustomPayload {
    public static final Id<QueuePayload> ID = new Id<>(Identifier.of("minewatch", "queue"));
    public static final PacketCodec<RegistryByteBuf, QueuePayload> CODEC = PacketCodec.of(
            (v, b) -> { b.writeBoolean(v.inQueue); b.writeVarInt(v.queued); b.writeVarInt(v.min); b.writeVarInt(v.seconds + 1); },
            b -> new QueuePayload(b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readVarInt() - 1));
    public static final QueuePayload NONE = new QueuePayload(false, 0, 0, -1);

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
