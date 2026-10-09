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

        // 능력 아이콘 (하단 중앙 우측): 쿨다운은 시계 방향 부채꼴로 줄어든다
        int iy = h - 52, isz = 28;
        int bxI = w / 2 + 40, rxI = bxI + isz + 6;
        // 블링크: 충전 개수 + 다음 충전 진행
        abilityIcon(g, "blink", s.charges() > 0 ? "ready" : "off", bxI, iy, isz, s.charges() > 0 ? CYAN : 0xFF6A7C8C);
        label(g, "shift", s.charges() > 0, bxI + isz / 2 - 24, iy + isz + 1, 48, 20, 136);
        if (s.charges() < s.maxCharges()) HudEffects.cooldownSweep(g, bxI, iy, isz, 1f - s.chargeProgress(), 0x99000000);
        g.drawCenteredTextWithShadow(tr, String.valueOf(s.charges()), bxI + isz / 2, iy + isz - 11, WHITE);
        // 리콜
        abilityIcon(g, "recall", s.recalling() ? "active" : s.recallCooldown() > 0 ? "off" : "ready", rxI, iy, isz,
                s.recalling() ? WHITE : s.recallCooldown() > 0 ? 0xFF6A7C8C : CYAN);
        label(g, "e", s.recallCooldown() <= 0, rxI + isz / 2 - 10, iy + isz + 1, 20, 20, 58);
        HudEffects.cooldownSweep(g, rxI, iy, isz, s.recallCooldown(), 0x99000000);

        // 궁극기 원형 게이지 (하단 중앙)
        int ucx = w / 2, ucy = h - 38, pct = Math.round(s.ult() * 100);
        g.fill(ucx - 17, ucy - 17, ucx + 18, ucy + 18, 0x330B1220);
        HudEffects.ring(g, ucx, ucy, 20, 4, s.ult(), pct >= 100 ? ORANGE : 0xFFB8C4D0, 0x88222B38);
        g.drawCenteredTextWithShadow(tr, pct >= 100 ? "Q" : pct + "%", ucx, ucy - 4, pct >= 100 ? ORANGE : WHITE);

        HudEffects.renderCrosshair(g);
        HudEffects.renderHitMarker(g);
        HudEffects.renderDamageIndicators(g);
    }

    /** Seafle 능력 아이콘: 어두운 바탕(mul) 위에 밝은 형상(add)을 색조로 덧그린다. */
    private static void abilityIcon(DrawContext g, String ability, String state, int x, int y, int size, int tint) {
        Sprites.draw(g, Sprites.gui("hud/hud_ab_" + state + "_" + ability + "_mul"), x, y, size, size, 162, 162, 0xB0101828);
        Sprites.draw(g, Sprites.gui("hud/hud_ab_" + state + "_" + ability + "_add"), x, y, size, size, 162, 162, tint);
    }

    private static void label(DrawContext g, String key, boolean ready, int x, int y, int w, int h, int texW) {
        String st = ready ? "ready" : "dim";
        Sprites.draw(g, Sprites.gui("hud/hud_ab_lbl_" + key + "_" + st + "_mul"), x, y, w, h, texW, 58, 0xB0101828);
        Sprites.draw(g, Sprites.gui("hud/hud_ab_lbl_" + key + "_" + st + "_add"), x, y, w, h, texW, 58, ready ? 0xFFFFFFFF : 0xFF8899AA);
    }
    private OverwatchHud() {}
}
