package com.minewatch.client;

import com.minewatch.GeoWeaponItem;
import com.minewatch.net.InputPayload;
import com.minewatch.net.StatePayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

/** 서버 상태/입력의 변화를 GeckoLib 1인칭 무기 애니메이션으로 바꿔 재생한다(클라이언트에서 바로). */
final class WeaponAnims {
    private static int lastAmmo = -1, lastCharges = -1, lastButtons;
    private static boolean lastReloading, lastRecall, left;

    static void reset() { lastAmmo = -1; lastCharges = -1; lastReloading = false; lastRecall = false; }

    static void tick(MinecraftClient mc, int buttons) {
        ItemStack st = mc.player.getMainHandStack();
        StatePayload s = MineWatchClient.state;
        if (!(st.getItem() instanceof GeoWeaponItem g) || s.heroId() == 0) { reset(); lastButtons = buttons; return; }

        boolean tracer = g.animName.equals("tracer");
        if (lastAmmo >= 0 && s.ammo() < lastAmmo && !s.reloading()) {
            g.playLocal(st, tracer ? (left ? "fire_left" : "fire_right") : "fire");
            left = !left;
        }
        if (s.reloading() && !lastReloading) g.playLocal(st, "reload");

        int charges = -1; boolean recall = false;
        for (StatePayload.Slot slot : s.slots()) {
            if (slot.icon().equals("blink")) charges = slot.charges();
            if (slot.icon().equals("recall")) recall = slot.active();
        }
        if (lastCharges >= 0 && charges >= 0 && charges < lastCharges) g.playLocal(st, "blink");
        if (recall && !lastRecall) g.playLocal(st, "recall");

        int rising = buttons & ~lastButtons;
        if ((rising & InputPayload.MELEE) != 0) g.playLocal(st, tracer ? "melee" : "swing");
        if ((rising & InputPayload.FIRE) != 0 && s.maxAmmo() == 0) g.playLocal(st, "swing");     // 탄창 없는 근접/지팡이 영웅
        if ((rising & InputPayload.ULT) != 0 && s.ult() >= 1f) g.playLocal(st, "bomb");

        lastAmmo = s.ammo(); lastReloading = s.reloading(); lastCharges = charges; lastRecall = recall; lastButtons = buttons;
    }
    private WeaponAnims() {}
}
