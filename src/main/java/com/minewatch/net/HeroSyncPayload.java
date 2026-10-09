package com.minewatch.net;

import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** 서버 -> 모든 클라이언트: 어떤 플레이어가 어떤 영웅인지(0 = 영웅 없음). 영웅 스킨을 입히는 데 쓴다. */
public record HeroSyncPayload(UUID player, int heroId) implements CustomPayload {
    public static final Id<HeroSyncPayload> ID = new Id<>(Identifier.of("minewatch", "hero_sync"));
    public static final PacketCodec<RegistryByteBuf, HeroSyncPayload> CODEC = PacketCodec.of(
            (v, b) -> { b.writeUuid(v.player); b.writeVarInt(v.heroId); },
            b -> new HeroSyncPayload(b.readUuid(), b.readVarInt()));

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
