package com.minewatch.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** 서버 -> 클라이언트: HUD 에 필요한 영웅 상태. heroId 0 = 영웅 없음. */
public record StatePayload(int heroId, int ammo, int maxAmmo, boolean reloading,
                           int charges, int maxCharges, float chargeProgress,
                           float ult, boolean recalling, float recallCooldown) implements CustomPayload {
    public static final Id<StatePayload> ID = new Id<>(Identifier.of("minewatch", "state"));
    public static final PacketCodec<RegistryByteBuf, StatePayload> CODEC = PacketCodec.of(
            (v, b) -> {
                b.writeVarInt(v.heroId); b.writeVarInt(v.ammo); b.writeVarInt(v.maxAmmo); b.writeBoolean(v.reloading);
                b.writeVarInt(v.charges); b.writeVarInt(v.maxCharges); b.writeFloat(v.chargeProgress);
                b.writeFloat(v.ult); b.writeBoolean(v.recalling); b.writeFloat(v.recallCooldown);
            },
            b -> new StatePayload(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readBoolean(),
                    b.readVarInt(), b.readVarInt(), b.readFloat(),
                    b.readFloat(), b.readBoolean(), b.readFloat()));

    public static final StatePayload NONE = new StatePayload(0, 0, 0, false, 0, 0, 0, 0, false, 0);

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
