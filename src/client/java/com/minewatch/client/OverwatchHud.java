package com.minewatch.client;

import com.minewatch.hero.Hero;
import com.minewatch.hero.HeroRegistry;
import com.minewatch.net.PoolsPayload;
import com.minewatch.net.StatePayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

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
        for (StatePayload.Slot sl : s.slots()) if (sl.icon().equals("recall") && sl.active()) g.fill(0, 0, w, h, 0x4400AAFF);

        // 체력 + 방어구(노랑) + 보호막(파랑). 칸 하나 = 10 HP, 최대치가 크면 칸이 좁아진다.
        PoolsPayload pl = MineWatchClient.pools;
        int hpOw = Math.round(mc.player.getHealth() * (float) Hero.HP_SCALE);
        int maxOw = Math.round(mc.player.getMaxHealth() * (float) Hero.HP_SCALE);
        int bx = 20, by = h - 36;
        int widest = Math.max(maxOw, Math.max(pl.maxArmor(), pl.maxShield()));
        int availW = Math.max(60, w / 2 - 70 - bx);              // 화면 중앙의 궁극기 링을 침범하지 않게
        int cw = Math.max(2, Math.min(10, availW / Math.max(1, widest / 10)));
        int rows = 1 + (pl.maxArmor() > 0 ? 1 : 0) + (pl.maxShield() > 0 ? 1 : 0);
        g.fill(bx - 2, by - 2 - (rows - 1) * 7, bx + (widest / 10) * cw + 2, by + 12, DARK);
        drawRow(g, bx, by, cw, 10, hpOw, maxOw, WHITE, 0x55FFFFFF);
        int ry = by - 7;
        if (pl.maxArmor() > 0) { drawRow(g, bx, ry, cw, 5, pl.armor(), pl.maxArmor(), ARMOR, 0x55F2C94C); ry -= 7; }
        if (pl.maxShield() > 0) { drawRow(g, bx, ry, cw, 5, pl.shield(), pl.maxShield(), SHIELD, 0x554DB8FF); }
        g.drawText(tr, String.valueOf(hpOw + pl.armor() + pl.shield()), bx, by - 14 - (rows - 1) * 7, WHITE, true);

        // 퍽 경험치 바 + 선택 알림
        var pk = MineWatchClient.perk;
        if (pk.next() > 0) {
            int px = 20, py = h - 20, pw = 150;
            g.fill(px - 1, py - 1, px + pw + 1, py + 4, DARK);
            g.fill(px, py, px + Math.min(pw, Math.round(pw * pk.xp() / (float) pk.next())), py + 3, pk.stage() == 1 || pk.stage() == 3 ? ORANGE : 0xFF8FB4D8);
        }
        if (pk.stage() == 1 || pk.stage() == 3) {
            if ((System.currentTimeMillis() / 500) % 2 == 0) g.drawText(tr, Text.translatable("hud.minewatch.perk_available"), 20, h - 32 - 12 * 3, ORANGE, true);
        }

        // 탄약 (우하단): 탄창이 있는 영웅만
        if (s.maxAmmo() > 0) {
            String ammo = s.reloading() ? "RELOAD" : s.ammo() + " / " + s.maxAmmo();
            g.drawText(tr, ammo, w - 30 - tr.getWidth(ammo) * 2, h - 40, s.ammo() <= s.maxAmmo() / 4 ? 0xFFFF5555 : WHITE, true);
        }
        Hero hero = HeroRegistry.get(s.heroId());
        if (s.extra() > 0 && hero != null)
            g.drawText(tr, Text.translatable("hud.minewatch.extra." + hero.id, s.extra()), w - 30 - 90, h - 54, ORANGE, true);

        // 능력 아이콘 (하단 중앙 우측): 쿨다운은 시계 방향 부채꼴로 줄어든다
        int iy = h - 52, isz = 28;
        for (int i = 0; i < Math.min(2, s.slots().size()); i++) {
            StatePayload.Slot slot = s.slots().get(i);
            int ix = w / 2 + 40 + i * (isz + 6);
            boolean ready = slot.cooldown() <= 0 && (slot.maxCharges() == 0 || slot.charges() > 0);
            if (slot.icon().equals("blink") || slot.icon().equals("recall")) {
                String st = slot.active() ? "active" : ready ? "ready" : "off";
                if (slot.icon().equals("blink")) st = ready ? "ready" : "off";
                abilityIcon(g, slot.icon(), st, ix, iy, isz, slot.active() ? WHITE : ready ? CYAN : 0xFF6A7C8C);
            } else {
                // Seafle 스프라이트가 없는 능력: 생성한 흰색 아이콘을 상태 색으로 칠한다
                int tintC = slot.active() ? WHITE : ready ? CYAN : 0xFF6A7C8C;
                g.fill(ix - 1, iy - 1, ix + isz + 1, iy + isz + 1, tintC);
                g.fill(ix, iy, ix + isz, iy + isz, DARK);
                Sprites.draw(g, Sprites.gui("ability/" + slot.icon()), ix + 3, iy + 3, isz - 6, isz - 6, 64, 64, tintC);
            }
            if (slot.cooldown() > 0 && slot.maxCharges() == 0) HudEffects.cooldownSweep(g, ix, iy, isz, slot.cooldown(), 0x99000000);
            if (slot.maxCharges() > 0) {
                if (slot.charges() < slot.maxCharges()) HudEffects.cooldownSweep(g, ix, iy, isz, slot.cooldown(), 0x99000000);
                g.drawCenteredTextWithShadow(tr, String.valueOf(slot.charges()), ix + isz / 2, iy + isz - 11, WHITE);
            }
            if (i == 0) label(g, "shift", ready, ix + isz / 2 - 24, iy + isz + 1, 48, 20, 136);
            else label(g, "e", ready, ix + isz / 2 - 10, iy + isz + 1, 20, 20, 58);
        }

        // 궁극기 원형 게이지 (하단 중앙)
        int ucx = w / 2, ucy = h - 38, pct = Math.round(s.ult() * 100);
        g.fill(ucx - 17, ucy - 17, ucx + 18, ucy + 18, 0x330B1220);
        HudEffects.ring(g, ucx, ucy, 20, 4, s.ult(), pct >= 100 ? ORANGE : 0xFFB8C4D0, 0x88222B38);
        g.drawCenteredTextWithShadow(tr, pct >= 100 ? "Q" : pct + "%", ucx, ucy - 4, pct >= 100 ? ORANGE : WHITE);

        if (s.scoped()) HudEffects.renderScope(g); else HudEffects.renderCrosshair(g);
        HudEffects.renderHitMarker(g);
        HudEffects.renderDamageIndicators(g);
    }

    /** Seafle 능력 아이콘: 어두운 바탕(mul) 위에 밝은 형상(add)을 색조로 덧그린다. */
    private static void abilityIcon(DrawContext g, String ability, String state, int x, int y, int size, int tint) {
        Sprites.mul(g, Sprites.gui("hud/hud_ab_" + state + "_" + ability + "_mul"), x, y, size, size, 162, 162, 0xFFFFFF);
        Sprites.add(g, Sprites.gui("hud/hud_ab_" + state + "_" + ability + "_add"), x, y, size, size, 162, 162, dim(tint, 0.5f));
    }

    /** RGB 를 비율만큼 어둡게(가산 합성이 너무 밝아지지 않게). */
    private static int dim(int argb, float f) {
        int r = (int) (((argb >> 16) & 255) * f), gr = (int) (((argb >> 8) & 255) * f), b = (int) ((argb & 255) * f);
        return (r << 16) | (gr << 8) | b;
    }

    private static void label(DrawContext g, String key, boolean ready, int x, int y, int w, int h, int texW) {
        String st = ready ? "ready" : "dim";
        Sprites.mul(g, Sprites.gui("hud/hud_ab_lbl_" + key + "_" + st + "_mul"), x, y, w, h, texW, 58, 0xFFFFFF);
        Sprites.add(g, Sprites.gui("hud/hud_ab_lbl_" + key + "_" + st + "_add"), x, y, w, h, texW, 58, ready ? 0xFFFFFF : 0x8899AA);
    }
    private OverwatchHud() {}
}
