package com.minewatch.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** 힛마커/킬마커, 피해 방향 표시, 영웅 크로스헤어, 원형 게이지 그리기 도우미. */
final class HudEffects {
    private static volatile long hitTime;
    private static volatile int hitKind = -1;
    private record Dmg(double x, double z, long time) {}
    private static final List<Dmg> DAMAGE = new ArrayList<>();

    static void onHit(int kind) { hitKind = kind; hitTime = System.currentTimeMillis(); }
    static void onDamage(double x, double z) {
        synchronized (DAMAGE) { DAMAGE.add(new Dmg(x, z, System.currentTimeMillis())); if (DAMAGE.size() > 8) DAMAGE.remove(0); }
    }
    static void reset() { hitKind = -1; synchronized (DAMAGE) { DAMAGE.clear(); } }

    private static int alpha(int rgb, float a) { return (Math.round(255 * Math.max(0, Math.min(1, a))) << 24) | (rgb & 0xFFFFFF); }

    static void renderCrosshair(DrawContext g) {
        int cx = g.getScaledWindowWidth() / 2, cy = g.getScaledWindowHeight() / 2, c = 0xFFFFFFFF;
        g.fill(cx - 9, cy, cx - 4, cy + 1, c);
        g.fill(cx + 4, cy, cx + 9, cy + 1, c);
        g.fill(cx, cy - 9, cx + 1, cy - 4, c);
        g.fill(cx, cy + 4, cx + 1, cy + 9, c);
        g.fill(cx, cy, cx + 1, cy + 1, c);
    }

    /** 조준경: 가운데 정사각 창만 남기고 바깥을 어둡게, 십자선 표시. */
    static void renderScope(DrawContext g) {
        int w = g.getScaledWindowWidth(), h = g.getScaledWindowHeight();
        int sq = (int) (h * 0.92), x0 = (w - sq) / 2, y0 = (h - sq) / 2, cx = w / 2, cy = h / 2;
        int black = 0xFF000000;
        g.fill(0, 0, x0, h, black); g.fill(x0 + sq, 0, w, h, black);
        g.fill(x0, 0, x0 + sq, y0, black); g.fill(x0, y0 + sq, x0 + sq, h, black);
        g.fill(x0, cy, x0 + sq, cy + 1, 0xCC000000); g.fill(cx, y0, cx + 1, y0 + sq, 0xCC000000);
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFF2020);
    }

    static void renderHitMarker(DrawContext g) {
        int kind = hitKind;
        if (kind < 0) return;
        long life = kind == 2 ? 700 : 250;
        float t = 1f - (System.currentTimeMillis() - hitTime) / (float) life;
        if (t <= 0) return;
        int cx = g.getScaledWindowWidth() / 2, cy = g.getScaledWindowHeight() / 2;
        int rgb = kind == 2 ? 0xFF3030 : kind == 1 ? 0xFFD040 : 0xFFFFFF;
        int len = kind == 2 ? 9 : 6, gap = kind == 2 ? 5 : 4;
        int col = alpha(rgb, t);
        for (int k = gap; k < gap + len; k++) {
            g.fill(cx + k, cy + k, cx + k + 1, cy + k + 1, col);
            g.fill(cx - k, cy + k, cx - k + 1, cy + k + 1, col);
            g.fill(cx + k, cy - k, cx + k + 1, cy - k + 1, col);
            g.fill(cx - k, cy - k, cx - k + 1, cy - k + 1, col);
        }
    }

    static void renderDamageIndicators(DrawContext g) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        long now = System.currentTimeMillis();
        int cx = g.getScaledWindowWidth() / 2, cy = g.getScaledWindowHeight() / 2;
        synchronized (DAMAGE) {
            DAMAGE.removeIf(d -> now - d.time > 1500);
            for (Dmg d : DAMAGE) {
                double dx = d.x - mc.player.getX(), dz = d.z - mc.player.getZ();
                double enemyYaw = Math.toDegrees(Math.atan2(-dx, dz));
                double rel = Math.toRadians(enemyYaw - mc.player.getYaw());
                double sx = Math.sin(rel), sy = -Math.cos(rel);       // 화면 위쪽이 정면
                float a = 1f - (now - d.time) / 1500f;
                int col = alpha(0xFF2020, a);
                for (int t = -8; t <= 8; t++) {
                    int px = (int) Math.round(cx + sx * 64 + (-sy) * t);
                    int py = (int) Math.round(cy + sy * 64 + sx * t);
                    g.fill(px - 1, py - 1, px + 2, py + 2, col);
                }
            }
        }
    }

    // ---- 원형 도우미 ----

    /** 시계 방향(위쪽 0) 기준 각도 비율 0~1. */
    private static double frac(int dx, int dy) {
        double a = Math.atan2(dx, -dy);
        if (a < 0) a += Math.PI * 2;
        return a / (Math.PI * 2);
    }

    /** 고리 게이지. frac 만큼 color 로, 나머지는 bg 로 칠한다. */
    static void ring(DrawContext g, int cx, int cy, int r, int thick, float fill, int color, int bg) {
        int inner = r - thick;
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                int d2 = dx * dx + dy * dy;
                if (d2 > r * r || d2 < inner * inner) continue;
                g.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, frac(dx, dy) <= fill ? color : bg);
            }
        }
    }

    /** 정사각 아이콘 위에 쿨다운 부채꼴(남은 비율 remaining)을 덮는다. */
    static void cooldownSweep(DrawContext g, int x, int y, int size, float remaining, int color) {
        if (remaining <= 0) return;
        int c = size / 2;
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                if (frac(px - c, py - c) < remaining) g.fill(x + px, y + py, x + px + 1, y + py + 1, color);
            }
        }
    }
    private HudEffects() {}
}
