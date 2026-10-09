package com.minewatch.client;

import com.minewatch.net.StatePayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/** 오버워치 스타일 HUD: 체력, 탄약, 블링크 충전, 리콜, 궁극기. */
final class OverwatchHud {
    private static final int WHITE = 0xFFFFFFFF, CYAN = 0xFF00D4FF, ORANGE = 0xFFF99E1A, DARK = 0xAA0B1220;

    static void render(DrawContext g, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        StatePayload s = MineWatchClient.state;
        if (s.heroId() == 0 || mc.player == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;
        int w = g.getScaledWindowWidth(), h = g.getScaledWindowHeight();

        // 리콜 중 파란 화면 섬광
        if (s.recalling()) g.fill(0, 0, w, h, 0x4400AAFF);

        // 체력 (150 HP 기준)
        float max = mc.player.getMaxHealth();
        float hp = mc.player.getHealth();
        int hpOw = Math.round(hp * 150f / max);
        int bx = 20, by = h - 36, bw = 150;
        g.fill(bx - 2, by - 2, bx + bw + 2, by + 12, DARK);
        for (int i = 0; i < 15; i++) {   // 10 HP 단위 칸
            int x0 = bx + i * 10;
            boolean on = hpOw > i * 10;
            g.fill(x0, by, x0 + 9, by + 10, on ? WHITE : 0x55FFFFFF);
        }
        g.drawText(tr, String.valueOf(hpOw), bx, by - 14, WHITE, true);

        // 탄약 (우하단)
        String ammo = s.reloading() ? "RELOAD" : s.ammo() + " / " + s.maxAmmo();
        g.drawText(tr, ammo, w - 30 - tr.getWidth(ammo) * 2, h - 40, s.ammo() <= s.maxAmmo() / 4 ? 0xFFFF5555 : WHITE, true);

        // 어빌리티 (하단 중앙 우측)
        int ax = w / 2 + 90, ay = h - 38;
        // 블링크 충전
        for (int i = 0; i < s.maxCharges(); i++) {
            int x0 = ax + i * 14;
            g.fill(x0, ay, x0 + 12, ay + 12, DARK);
            if (i < s.charges()) g.fill(x0 + 1, ay + 1, x0 + 11, ay + 11, CYAN);
            else if (i == s.charges()) { int fillH = Math.round(10 * s.chargeProgress()); g.fill(x0 + 1, ay + 11 - fillH, x0 + 11, ay + 11, 0x8800D4FF); }
        }
        g.drawText(tr, "Shift Blink", ax, ay + 15, WHITE, true);
        // 리콜 쿨다운
        int rx = ax + 70;
        g.fill(rx, ay, rx + 26, ay + 12, DARK);
        if (s.recallCooldown() > 0) g.fill(rx + 1, ay + 1, rx + 1 + Math.round(24 * (1 - s.recallCooldown())), ay + 11, 0x66FFFFFF);
        else g.fill(rx + 1, ay + 1, rx + 25, ay + 11, CYAN);
        g.drawText(tr, "E Recall", rx - 4, ay + 15, WHITE, true);

        // 궁극기 게이지
        int ux = w / 2 - 14, uy = h - 56;
        int pct = Math.round(s.ult() * 100);
        g.fill(ux - 1, uy - 1, ux + 29, uy + 9, DARK);
        g.fill(ux, uy, ux + Math.round(28 * s.ult()), uy + 8, pct >= 100 ? ORANGE : 0xFF8899AA);
        String ut = pct >= 100 ? "Q ULT" : pct + "%";
        g.drawCenteredTextWithShadow(tr, ut, ux + 14, uy - 11, pct >= 100 ? ORANGE : WHITE);
    }
    private OverwatchHud() {}
}
