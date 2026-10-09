package com.minewatch.client;

import com.minewatch.hero.Hero;
import com.minewatch.net.PoolsPayload;
import com.minewatch.net.StatePayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/** 오버워치 스타일 HUD: 체력, 탄약, 블링크 충전, 리콜, 궁극기. */
final class OverwatchHud {
    private static final int WHITE = 0xFFFFFFFF, CYAN = 0xFF00D4FF, ORANGE = 0xFFF99E1A, DARK = 0xAA0B1220;

    private static final int ARMOR = 0xFFF2C94C, SHIELD = 0xFF4DB8FF;

    private static void drawRow(DrawContext g, int x, int y, int cw, int ch, int value, int max, int on, int off) {
        for (int i = 0; i * 10 < max; i++) {
            int x0 = x + i * cw;
            g.fill(x0, y, x0 + cw - 1, y + ch, value > i * 10 ? on : off);
        }
    }

    static void render(DrawContext g, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        StatePayload s = MineWatchClient.state;
        if (s.heroId() == 0 || mc.player == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;
        int w = g.getScaledWindowWidth(), h = g.getScaledWindowHeight();

        // 리콜 중 파란 화면 섬광
        if (s.recalling()) g.fill(0, 0, w, h, 0x4400AAFF);

        // 체력 + 방어구(노랑) + 보호막(파랑). 칸 하나 = 10 HP, 최대치가 크면 칸이 좁아진다.
        PoolsPayload pl = MineWatchClient.pools;
        int hpOw = Math.round(mc.player.getHealth() * (float) Hero.HP_SCALE);
        int maxOw = Math.round(mc.player.getMaxHealth() * (float) Hero.HP_SCALE);
        int bx = 20, by = h - 36;
        int widest = Math.max(maxOw, Math.max(pl.maxArmor(), pl.maxShield()));
        int cw = Math.max(2, Math.min(10, 220 / Math.max(1, widest / 10)));
        int rows = 1 + (pl.maxArmor() > 0 ? 1 : 0) + (pl.maxShield() > 0 ? 1 : 0);
        g.fill(bx - 2, by - 2 - (rows - 1) * 7, bx + (widest / 10) * cw + 2, by + 12, DARK);
        drawRow(g, bx, by, cw, 10, hpOw, maxOw, WHITE, 0x55FFFFFF);
        int ry = by - 7;
        if (pl.maxArmor() > 0) { drawRow(g, bx, ry, cw, 5, pl.armor(), pl.maxArmor(), ARMOR, 0x55F2C94C); ry -= 7; }
        if (pl.maxShield() > 0) { drawRow(g, bx, ry, cw, 5, pl.shield(), pl.maxShield(), SHIELD, 0x554DB8FF); }
        g.drawText(tr, String.valueOf(hpOw + pl.armor() + pl.shield()), bx, by - 14 - (rows - 1) * 7, WHITE, true);

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
