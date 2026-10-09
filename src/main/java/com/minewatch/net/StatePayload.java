package com.minewatch.net;

import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 서버 -> 클라이언트: HUD 에 필요한 영웅 상태. heroId 0 = 영웅 없음.
 * slots 는 능력 아이콘(Shift, E 순서), scoped 는 조준경 사용 여부, extra 는 영웅별 보조 수치(%, 방벽 등).
 */
public record StatePayload(int heroId, int ammo, int maxAmmo, boolean reloading, float ult,
                           boolean scoped, int extra, List<Slot> slots) implements CustomPayload {

    /** 능력 한 칸. cooldown 은 남은 재사용 비율(0 = 사용 가능), charges/maxCharges 는 충전식 능력용. */
    public record Slot(String icon, float cooldown, int charges, int maxCharges, boolean active) {
        public static Slot of(String icon, float cooldown) { return new Slot(icon, cooldown, 0, 0, false); }
    }

    public static final Id<StatePayload> ID = new Id<>(Identifier.of("minewatch", "state"));
    public static final PacketCodec<RegistryByteBuf, StatePayload> CODEC = PacketCodec.of(
            (v, b) -> {
                b.writeVarInt(v.heroId); b.writeVarInt(v.ammo); b.writeVarInt(v.maxAmmo); b.writeBoolean(v.reloading);
                b.writeFloat(v.ult); b.writeBoolean(v.scoped); b.writeVarInt(v.extra);
                b.writeVarInt(v.slots.size());
                for (Slot s : v.slots) {
                    b.writeString(s.icon, 24); b.writeFloat(s.cooldown); b.writeVarInt(s.charges);
                    b.writeVarInt(s.maxCharges); b.writeBoolean(s.active);
                }
            },
            b -> {
                int heroId = b.readVarInt(), ammo = b.readVarInt(), maxAmmo = b.readVarInt();
                boolean reloading = b.readBoolean();
                float ult = b.readFloat();
                boolean scoped = b.readBoolean();
                int extra = b.readVarInt();
                int n = Math.min(b.readVarInt(), 4);
                java.util.ArrayList<Slot> slots = new java.util.ArrayList<>();
                for (int i = 0; i < n; i++)
                    slots.add(new Slot(b.readString(24), b.readFloat(), b.readVarInt(), b.readVarInt(), b.readBoolean()));
                return new StatePayload(heroId, ammo, maxAmmo, reloading, ult, scoped, extra, List.copyOf(slots));
            });

    public static final StatePayload NONE = new StatePayload(0, 0, 0, false, 0, false, 0, List.of());

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
