package com.minewatch.net;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** 클라이언트 -> 서버: 매 틱 입력 상태 (비트 플래그 + 이동 입력). */
public record InputPayload(int buttons, float forward, float sideways) implements CustomPayload {
    public static final int FIRE = 1, RELOAD = 1 << 1, ABILITY1 = 1 << 2, ABILITY2 = 1 << 3,
            ULT = 1 << 4, MELEE = 1 << 5;

    public static final Id<InputPayload> ID = new Id<>(Identifier.of("minewatch", "input"));
    public static final PacketCodec<RegistryByteBuf, InputPayload> CODEC = PacketCodec.of(
            (v, buf) -> { buf.writeByte(v.buttons); buf.writeFloat(v.forward); buf.writeFloat(v.sideways); },
            buf -> new InputPayload(buf.readByte(), buf.readFloat(), buf.readFloat()));

    public static final InputPayload EMPTY = new InputPayload(0, 0, 0);

    public boolean has(int flag) { return (buttons & flag) != 0; }

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
